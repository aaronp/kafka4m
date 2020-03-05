package kafka4m.consumer

import cats.effect.concurrent.Ref
import cats.syntax.apply._
import kafka4m.admin.ConsumerGroupStats
import kafka4m.consumer.ConcurrentStream.ZipOffset
import kafka4m.consumer.HasOffset.Const
import kafka4m.data.PartitionOffsetState
import kafka4m.producer.RichKafkaProducer
import kafka4m.util.{Schedulers, Using}
import kafka4m.{BaseKafka4mDockerSpec, Kafka4mTestConfig}
import monix.eval.Task
import monix.execution.ExecutionModel
import monix.reactive.Observable

import scala.concurrent.Future
import scala.concurrent.duration._

class ConcurrentStreamTest extends BaseKafka4mDockerSpec {

  "ConcurrentStream.commit" should {
    "not commit until the min commit is met" in {
      Schedulers.using(Schedulers.io(executionModel = ExecutionModel.AlwaysAsyncExecution)) { implicit taskScheduler =>
        val totalExpected = 10
        val inputs = {
          val records: Seq[Const[Int]] = (0 to totalExpected).map { i =>
            HasOffset.Const(i, PartitionOffsetState(("topic", Map(0 -> i.toLong))))
          }
          Observable.fromIterable(records).delayOnNext(5.milliseconds)
        }
        object FakeKafka extends ConcurrentStream.KafkaFacade {
          val committedOffsets: Ref[Task, List[Long]] = Ref.unsafe[Task, List[Long]](Nil)
          override def commit(offsetState: PartitionOffsetState): Task[Unit] = {
            committedOffsets.update { list =>
              val List(offset) = offsetState.offsetByPartitionByTopic.values.flatMap(_.values).toList
              list :+ offset
            }
          }
        }

        case class TestState(localOffset: Int, committed: List[Long], data: Int)

        val onlyCommitEveryTen: ConcurrentStream[Const[Int]] = ConcurrentStream(inputs, taskScheduler, FakeKafka, 3)

        val balanced: Observable[(ZipOffset, Const[Int], Int)] = onlyCommitEveryTen.loadBalance(1) { input: Const[Int] =>
          Task(input.data)
        }

        val testState: Observable[TestState] = balanced.mapEval {
          case (offset: ZipOffset, data: Const[Int], _) =>
            FakeKafka.committedOffsets.get.map { committed =>
              TestState(offset.toInt, committed, data.data)
            }
        }
        val results: List[TestState] = testState.toListL.runSyncUnsafe()
        results shouldBe List(
          TestState(0, List(), 0),
          TestState(1, List(), 1),
          TestState(2, List(), 2),
          TestState(3, List(), 3),
          TestState(4, List(3), 4),
          TestState(5, List(3), 5),
          TestState(6, List(3), 6),
          TestState(7, List(3, 6), 7),
          TestState(8, List(3, 6), 8),
          TestState(9, List(3, 6), 9),
          TestState(10, List(3, 6, 9), 10)
        )
      }
    }
  }
  "ConcurrentStream" should {
    "commit offsets in the same thread" in {

      Schedulers.using(Schedulers.io(executionModel = ExecutionModel.AlwaysAsyncExecution)) { implicit taskScheduler =>
        val totalExpected = 100
        val records: Seq[Const[Int]] = (0 to totalExpected).map { i =>
          HasOffset.Const(i, PartitionOffsetState(("topic", Map(0 -> i.toLong))))
        }

        val inputs = Observable.fromIterable(records).delayOnNext(5.milliseconds)

        object FakeKafka extends ConcurrentStream.KafkaFacade {
          @volatile var latestCommittedOffset                             = 0
          val commitsRef: Ref[Task, List[(String, PartitionOffsetState)]] = Ref.unsafe[Task, List[(String, PartitionOffsetState)]](Nil)

          override def commit(offset: PartitionOffsetState): Task[Unit] = {
            // out test data always fix this topic and partition
            latestCommittedOffset = offset.offsetByPartitionByTopic("topic")(0).toInt

            commitsRef.update { list =>
              val threadName = Thread.currentThread().getName
              val entry      = (threadName, offset)
              entry :: list
            }
          }
        }

        val underTest: ConcurrentStream[Const[Int]] = ConcurrentStream(inputs, taskScheduler, FakeKafka, 0)

        val balanced = underTest.loadBalance(10) { const: Const[Int] =>
          val record = const.data
          val wait   = (record % 5) * 20
          import cats.syntax.apply._
          val task = Task(logger.info(s"$record waiting for $wait ms")) *> Task.sleep(wait.milliseconds) *> Task {
            val threadName = Thread.currentThread().getName
            val msg        = s"$record done on $threadName!"
            logger.info(msg)
            threadName
          }

          task.executeOn(taskScheduler, true)
        }

        withClue("thread jobs should be executed in parallel on different threads") {
          val threads = balanced.toListL.runSyncUnsafe(testTimeout)
          threads.distinct.size should be > 1
        }

        withClue("all commits should happen on a single thread") {
          val commitList: Seq[(String, PartitionOffsetState)] = eventually {
            val list: Seq[(String, PartitionOffsetState)] = FakeKafka.commitsRef.get.runSyncUnsafe(testTimeout)
            list.size should be > 1
            list
          }

          commitList.sliding(2, 1).foreach {
            case Seq((_, laterOffsets), (_, earlierOffsets)) =>
              PartitionOffsetState.checkOffsets(earlierOffsets, laterOffsets) shouldBe None
          }
        }

        eventually {
          FakeKafka.latestCommittedOffset shouldBe totalExpected
        }
      }
    }

    "not commit offsets when jobs fail" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next(closeOnComplete = false)

        Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
          val totalMsgs = 50
          val futures = (0 until totalMsgs).map { i =>
            producer.sendAsync(topic, i.toString, i.toString.getBytes())
          }
          Future.sequence(futures).futureValue

          val failAfter = (totalMsgs * 0.75).toInt

          val stream = kafka4m.loadBalance[String, String](config, parallelism = 10) { input =>
            val millis = 10 + ((input.toInt % 5) * 50)

            Task.sleep(millis.millis) *> Task {
              require(input.toInt < failAfter, s"This task is a gonna fail as expected on $failAfter")
              val msg = s"$input finished after $millis"
              logger.info(msg)
              msg
            }
          }

          val bang: Exception = intercept[Exception] {
            stream.take(totalMsgs).lastL.runToFuture.futureValue
          }
          bang.getMessage should include(s"This task is a gonna fail as expected on $failAfter")

          Using(kafka4m.richAdmin(config)) { admin =>
            val stats: Seq[ConsumerGroupStats] = admin.consumerGroupsStats.futureValue
            val List(topicStatus)              = stats.filter(_.topics.contains(topic))
            topicStatus.offsetsByPartition(0).toInt should be > 0
            topicStatus.offsetsByPartition(0).toInt should be <= failAfter
          }
        }
      }
    }
    "commit offsets when the futures complete" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next(closeOnComplete = false)

        Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
          val totalMsgs = 50
          val futures = (0 until totalMsgs).map { i =>
            producer.sendAsync(topic, i.toString, i.toString.getBytes())
          }
          Future.sequence(futures).futureValue

          val stream = kafka4m.loadBalance[String, String](config, parallelism = 10) { input =>
            val millis = 10 + ((input.toInt % 5) * 50)

            Task.sleep(millis.millis) *> Task {
              val msg = s"$input finished after $millis"
              logger.info(msg)
              msg
            }
          }

          val last: ComputeResult[String, String] = stream.take(totalMsgs).lastL.runToFuture.futureValue
          val commits                             = last.withConsumer(_.committedStatus()).futureValue
          withClue(s"Found commits: $commits") {
            commits should not be empty
            val List(subscribed) = commits.filter(_.subscribed)
            subscribed.offsets(0).toInt should be > 0
            subscribed.offsets(0).toInt should be <= totalMsgs
          }
        }
      }
    }
  }
}
