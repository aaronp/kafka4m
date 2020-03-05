package kafka4m.consumer

import cats.effect.concurrent.Ref
import cats.syntax.apply._
import com.typesafe.scalalogging.StrictLogging
import kafka4m.AckBytes
import kafka4m.consumer.ConcurrentStream._
import kafka4m.data.PartitionOffsetState
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ConcurrentStream {

  type ZipOffset   = Long
  type OffsetState = (ZipOffset, Try[PartitionOffsetState])

  /**
    * A local facade just encapsulating the functionality we need to commit offsets to kafka
    */
  trait KafkaFacade extends AutoCloseable {
    def commit(offset: PartitionOffsetState): Task[Unit]
    override def close() = {}
  }

  object KafkaFacade {
    def apply(access: ConsumerAccess): KafkaFacade = new KafkaFacade {
      override def commit(offset: PartitionOffsetState): Task[Unit] = {
        Task.defer(Task.fromFuture(access.withConsumer(_.commitAsync(offset))))
      }

      override def close(): Unit = access.withConsumer(_.close())
    }
  }
}

/**
  * Kafka consumer access is enforced to be single-threaded, and you can understand why.
  *
  * Suppose a consumer was to read ten messages from kafka and send off ten async requests.
  *
  * If the tenth request happened to come back first and commit its offset, then what about the other nine which might fail?
  *
  * On the flip-side, if we were to block on each async call for every message, that would be a performance killer, and
  * unnecessary if the calls are idempotent.
  *
  * To enable async handling/commits, we just need to ensure we cater for this case:
  *
  * {{{
  * msg1 -------------->+
  *                     |
  * msg2 ----->+        |
  *            |      !bang!
  *  ok  <-----+        |
  *                     |
  *      <- onFailure --+
  * }}}
  *
  * we shouldn't commit the offset for msg2, even though it succeeded first.
  *
  * The way we handle this is by having the futures drive a [[ConcurrentSubject]] of offsets zipped with the messages we receive from Kafka.
  *
  * {{{
  * msg1 --------------> ???
  * msg2 --------------> ???
  * msg3 --------------> ???
  * msg4 --------------> ???
  * msg5 --------------> ???
  * msg6 --------------> ???
  *
  * ... some mixed order - just ensuring we do get either a failure or a success for each result
  *
  * msg6 <--------- ???
  * msg2 <--------- ???
  * msg5 <--------- ???
  * msg1 <--------- ??? // here we can commit up to offset 2 as 1 and 2 have returned
  *
  * }}}
  *
  *
  *
  * @param kafkaData the data coming from kafka
  * @param asyncScheduler the scheduler to use in running tasks
  * @param kafkaFacade our means of committing offsets to kafka
  * @param minCommitFrequency how frequently we'll try and commit offsets to kafka. Set to 0 to commit as soon as tasks complete successfully
  * @param awaitJobTimeout the amount of time to wait for the last job to complete when trying to commit the last position back to kafka
  * @param retryDuration the "poll time" when checking for the result of the final task
  * @tparam A the messages in the kafka feed (typically [[AckableRecord]]s)
  */
case class ConcurrentStream[A](kafkaData: Observable[A],
                               asyncScheduler: Scheduler,
                               kafkaFacade: KafkaFacade,
                               minCommitFrequency: Int,
                               awaitJobTimeout: FiniteDuration = 10.seconds,
                               retryDuration: FiniteDuration = 50.milliseconds)(implicit val hasOffset: HasOffset[A])
    extends StrictLogging
    with AutoCloseable {

  /**
    * Converts the kafkaData to a 'B' type, provided there is a 'HasOffset' for B so we know what to commit back
    * to Kafka
    *
    * @param ev
    * @param decoder
    * @param newHasOffset
    * @tparam B
    * @return
    */
  def as[B](implicit ev: A =:= AckBytes, decoder: RecordDecoder.ByteArrayDecoder[B], newHasOffset: HasOffset[B]): ConcurrentStream[B] = {
    map(r => decoder.decode(r.record))
  }

  /**
    * Convenience method for 'map' which uses a decoder to B.
    * This maps inner the kafkaData to the B type, but still wrapped in an [[AckableRecord]]
    *
    * @param ev
    * @param decoder
    * @tparam B
    * @return a ConcurrentStream of type 'B'
    */
  def decode[B](implicit ev: A =:= AckBytes, decoder: RecordDecoder.ByteArrayDecoder[B]): ConcurrentStream[AckableRecord[B]] = {
    map(r => r.map(decoder.decode))
  }

  /**
    * Maps the kafkaData to the B type, but still wrapped in an [[AckableRecord]]
    * @param f
    * @param newHasOffset
    * @tparam B
    * @return
    */
  def map[B](f: A => B)(implicit newHasOffset: HasOffset[B]): ConcurrentStream[B] = {
    copy(kafkaData.map(f))
  }

  /**
    * Convenience method which combines the 'decode' and 'loadBalance' functions
    * @param parallelism
    * @param compute
    * @param ev
    * @param decoder
    * @tparam B
    * @tparam C
    * @return
    */
  def compute[B, C](parallelism: Int)(compute: B => Task[C])(implicit ev: A =:= AckBytes, decoder: RecordDecoder.ByteArrayDecoder[B]): Observable[ComputeResult[B, C]] = {
    val taskStream = decode[B].loadBalance(parallelism) { record =>
      compute(record.record)
    }
    taskStream.map {
      case (offset, record, response) => ComputeResult(offset, record, response)
    }
  }

  // our means of exchange between the tasks we're running on our inputs and the kafka feed which is trying to read from
  // this state so that the kafka feed can commit the positions
  private val latestCommittedOffsetSubject = ConcurrentSubject.publish[OffsetState](asyncScheduler)
  // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  //                 |
  // the reference being updated by this
  private val latestCommittedOffset = Ref.unsafe[Task, Option[OffsetState]](None)

  // the last offset we committed
  private val mostRecentlyCommittedOffset = Ref.unsafe[Task, Option[ZipOffset]](None)
  private def canAdvanceMostRecentCommittedOffset(kafkaIndex: ZipOffset): Task[Boolean] = {
    mostRecentlyCommittedOffset.get.map {
      case None =>
        logger.info(s"Checking first advance commit: $kafkaIndex >= $minCommitFrequency")
        kafkaIndex >= minCommitFrequency
      case Some(previous) =>
        val ok = kafkaIndex >= previous + minCommitFrequency
        logger.info(s"Checking advance commit yields $ok as $kafkaIndex >= $previous + $minCommitFrequency")
        ok
    }
  }
  private def updateLatestCommit(kafkaIndex: ZipOffset): Task[Unit] = {
    mostRecentlyCommittedOffset.update {
      case None => Option(kafkaIndex)
      case Some(last) if last < kafkaIndex =>
        logger.info(s"Updating last commit to $last")
        Option(kafkaIndex)
      case opt => opt
    }
  }

  // we need to hold on to a message so that we can commit on complete (yuck)
  private val latestMessage = Ref.unsafe[Task, Option[(A, ZipOffset)]](None)

  /**
    * A task which populates the 'latestMessage' Ref based on the sorted response stream coming from 'latestCommittedOffsetSubject'
    *
    * This task is started when the compute stream is subscribed to
    */
  private lazy val updateLatestPosition: Task[Unit] = {
    def updateLatest(offsetState: OffsetState): Task[Unit] = {
      latestCommittedOffset.update {
        case None                                                     => Option(offsetState)
        case Some((earlierState, _)) if earlierState < offsetState._1 => Option(offsetState)
        case other                                                    => other
      }.void
    }

    val sortedCommits      = ContiguousOrdering.sort(latestCommittedOffsetSubject)
    implicit val scheduler = asyncScheduler
    Task(sortedCommits.doOnNext(updateLatest).subscribe())
  }

  /**
    * Exposes a means to execute a task on each input from the kafka data.
    *
    * The tasks can be run in parallel, but the kafka offsets are only committed when *all* the tasks have completed successfully.
    *
    * Tasks in error will cause the Observable to fail, so if you want to continue consuming from Kafka after failures you will
    * need to ensure the Tasks have adequate error handling/retry logic built in.
    *
    * Example:
    *
    * Consider we kick off async processes after having consumed Kafka messages A,B,C,D and E :
    * {{{
    * A ---- start job on A -------------------+
    *                                          |
    * B ---- start job on B ---------------+   |
    *                                      |   |
    * C ---- start job on C -----------+   |   |
    *                                  |   |   |
    * D ---- start job on D ---+---+   |   |   |
    *                              |   |   |   |
    * E ---- start job on E ----+  |   |   |   |
    *                           |  |   |   |   |
    * 1: (c) <------------------+--+---+   |   |
    *                           |  |       |   |
    * 2: (b) <------------------+--+-------+   |
    *                           |  |           |
    * 3: (a) <------------------+--+-----------+
    *                           |  |
    * 4: (e) <---- !BANG! ------+  |
    *                              |
    * 5: (d) <---------------------+
    *
    * }}}
    *
    *
    * So, we've kicked off 5 jobs based on the first 5 messages, all on different threads.
    *
    * 1: At this point, received a successful response from the third message (C).
    *    We DON'T commit the offset back to kafka, because the tasks from A or B may yet fail, in which case
    *    we expect the Observable stream to fail with their error, and thus have messages A, B, etc replayed upon reconnect.
    *
    *    We DO however emit a tuple message of (2, C, c) -- e.g. the local offset index '2', input message C and 'c'
    *    result from the task.
    *
    *    The results are emitted in the order of the first tasks to complete, not necessarily their input order.
    *    If the downstream systems need to reconstruct the original order, they can either broadcast to many from the original
    *    kafka stream or use [[ContiguousOrdering]] to put the messages back in kafka received order.
    *
    * 2: Next we receive a successful response from the second message (B).
    *    Just as before, we're still waiting on result A so DON'T commit the offset, but DO emit a record (1,B,b)
    *
    * 3: Finally we get our first response back from message A. We've now received messages A,B and C and so can commit
    *    the 'C' offset to Kafka (not just A). If we die and reconnect now, we should start from message C.
    *    We also emit message (0,A,a)
    *
    * 4: We get the error response from E (e) so we end the stream in error. If we had received the response from
    *    message 'D' instead we wouldn't tried to commit that offset to kafka as it was the next index after 'E'.
    *    Too bad, so sad - we error our stream and presumably our app dies or at least we close our kafka connection.
    *
    * 5: Our stream is errored, so this response is simply ignored.
    *
    *
    * @param parallelism the parallelism to use when executing the jobs
    * @param runJobOnNext our job logic - the tasks to execute on each kafka input message
    * @tparam B
    * @return
    */
  def loadBalance[B](parallelism: Int)(runJobOnNext: A => Task[B]): Observable[(ZipOffset, A, B)] = {

    def tryToCommitFinalPosition = {
      latestMessage.get.flatMap {
        case None => Task.unit
        case Some((_, lastZipOffset)) =>
          def awaitLastTaskBeforeCommit(deadline: Deadline): Task[Unit] = {
            latestCommittedOffset.get.flatMap {
              case Some((receivedZipIdx, Success(mostRecentSuccessfulCommit))) if receivedZipIdx == lastZipOffset =>
                // TODO - check 'previouslyCommittedOffset' against a 'min commit delta' just to save committing so often to kafka
                logger.debug("All done, committing last value")
                kafkaFacade.commit(mostRecentSuccessfulCommit) *> updateLatestCommit(receivedZipIdx)
              case None => Task.sleep(retryDuration) *> awaitLastTaskBeforeCommit(deadline)
              case Some((_, Failure(err))) =>
                logger.warn(s"Task failed, can't commit the last index: $err")
                Task.raiseError(err)
              case Some(latestOffset) =>
                if (deadline.hasTimeLeft()) {
                  Task.sleep(retryDuration) *> awaitLastTaskBeforeCommit(deadline)
                } else {
                  val err = new Exception(s"Won't commit the offsets '$latestOffset' to kafka as we timed out after $awaitJobTimeout")
                  Task.raiseError(err)
                }
            }
          }

          awaitLastTaskBeforeCommit(awaitJobTimeout.fromNow)
      }
    }

    /** @param arbitraryInputTuple some 'next' message.
      * @return
      */
    def tryToCommitLastSuccessfulPosition(arbitraryInputTuple: (A, ZipOffset)): Task[Unit] = {
      val updateLatestMessageTask = latestMessage.update {
        case _ => Some(arbitraryInputTuple)
      }
      val commitTask: Task[Task[Unit]] = latestCommittedOffset.modify {
        case None =>
          logger.trace("Commit skipping as latest committed is None")
          None -> Task.unit
        case Some((_, Failure(err))) =>
          logger.warn(s"An async task raised $err ")
          None -> Task.raiseError(err)
        case Some((kafkaIndex, Success(mostRecentSuccessfulCommit))) =>
          val commitTask = canAdvanceMostRecentCommittedOffset(kafkaIndex).flatMap {
            case true  => kafkaFacade.commit(mostRecentSuccessfulCommit) *> updateLatestCommit(kafkaIndex)
            case false => Task.unit
          }
          None -> commitTask
      }

      commitTask.flatten *> updateLatestMessageTask
    }

    // format: off
    val zippedFeed = kafkaData
      .zipWithIndex // we keep track of the order we receive stuff from kafka as that's our global contiguous ordering we'll commit on
      .executeOn(asyncScheduler) //             ensure our kafka data is always executed on a single executor
      .doOnNext(tryToCommitLastSuccessfulPosition) // ^^^^^^ which is important for our 'doOnNext' ^^^^^^^
      .doOnComplete(tryToCommitFinalPosition)      // <----- ... and let's not forget the last one!
      .doOnSubscribe(updateLatestPosition) // start our job which will write the latest sorted position
    // format: on

    zippedFeed.mapParallelUnordered(parallelism) {
      case (next, localOffset) =>
        val offset = hasOffset.offsetForValue(next)

        def makeResponse(taskResult: B) = (localOffset, next, taskResult)

        // execute our consumer task. These tasks can be set to run on a different scheduler
        runJobOnNext(next).map(makeResponse).doOnFinish { resultOpt =>
          val asTry = resultOpt match {
            case None        => Success(offset)
            case Some(error) => Failure(error)
          }
          Task.fromFuture(latestCommittedOffsetSubject.onNext(localOffset, asTry)).void
        }
    }
  }

  override def close(): Unit = {
    kafkaFacade.close()
  }
}
