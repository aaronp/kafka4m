package kafka4m.consumer

import java.util.Properties
import java.{time, util}

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.data.{CommittedStatus, KafkaPartitionInfo, PartitionOffsetState}
import kafka4m.util.{FixedScheduler, Schedulers}
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.{BufferCapacity, Scheduler}
import monix.reactive.Observable
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Deserializer

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * A means of driving a kafka-stream using the consumer (not kafka streaming) API
  */
final class RichKafkaConsumer[K, V] private (val consumer: KafkaConsumer[K, V],
                                             val defaultTopics: Set[String],
                                             val defaultPollTimeout: Duration,
                                             commandQueue: ConcurrentQueue[Task, ExecOnConsumer[K, V, _]],
                                             kafkaScheduler: Scheduler,
                                             val asyncScheduler: Scheduler,
                                             val rebalanceListener: RebalanceListener = new RebalanceListener,
                                             startPollingOnStart: Boolean = true)
    extends AutoCloseable
    with ConsumerAccess
    with StrictLogging { self =>

  override type Key   = K
  override type Value = V

  @volatile private var closed = false

  private val javaPollDuration: time.Duration = RichKafkaConsumer.asJavaDuration(defaultPollTimeout)

  def partitionsByTopic(limitToOurTopic: Boolean = false): Map[String, List[KafkaPartitionInfo]] = {
    val view = consumer.listTopics().asScala.view.mapValues(_.asScala.map(KafkaPartitionInfo.apply).toList)
    if (limitToOurTopic) {
      view.filterKeys(defaultTopics.contains).toMap
    } else {
      view.toMap
    }
  }

  def subscribe(topic: String): Unit = subscribe(Set(topic))

  def subscribe(topics: Set[String], listener: ConsumerRebalanceListener = null): Unit = {
    val callback = if (listener != null) {
      DelegateListener(rebalanceListener, listener)
    } else {
      rebalanceListener
    }
    logger.info(s"Subscribing to $topics")
    consumer.subscribe(topics.asJava, callback)
  }

  def partitions: List[KafkaPartitionInfo] = {
    val byTopic = partitionsByTopic(true)
    byTopic.valuesIterator.flatten.toList
  }

  /**
    * this poll is unsafe as it will fail if invoked from a different thread from which this consumer was created
    * @param timeout
    * @return the records pull from Kafka
    */
  def unsafePoll(timeout: time.Duration = javaPollDuration): Iterable[ConsumerRecord[K, V]] = {
    try {
      val records: ConsumerRecords[K, V] = consumer.poll(timeout)
      logger.debug(s"Got ${records.count()} records from ${records.partitions().asScala.mkString(s"[", ",", "]")}")
      records.asScala
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Poll threw $e")
        throw e
    }
  }

  private val NoResults = Observable.empty[ConsumerRecord[K, V]]

  val nextBatchIterable: Task[Iterable[ConsumerRecord[K, V]]] = Task.eval(unsafePoll()).executeOn(kafkaScheduler)

  val nextBatch: Task[Observable[ConsumerRecord[K, V]]] = nextBatchIterable.map { returned =>
    if (returned.isEmpty) {
      NoResults
    } else {
      Observable.fromIterable(returned)
    }
  }

  /**
    * This will poll for a single batch and exec commands which may or may not need to be executed
    */
  val poll: Task[Observable[ConsumerRecord[K, V]]] = {
    commandQueue.tryPoll.flatMap {
      // try and handle any explicit commands, but if none are queued, then fall-back to polling kafka
      case None =>  nextBatch
      case Some(exec: ExecOnConsumer[K, V, _]) =>
        Task(exec.run(self)).executeOn(kafkaScheduler).map(_ => NoResults)
    }
  }

  /**
    * Represent this consumer as an observable
    * @param closeOnComplete set to true if the underlying Kafka consumer should be closed when this observable completes
    */
  def asObservable(closeOnComplete: Boolean): Observable[ConsumerRecord[K, V]] = {
    val obs: Observable[ConsumerRecord[K, V]] = Observable.repeatEvalF(poll).flatten.observeOn(asyncScheduler)
    if (closeOnComplete) {
      obs.guarantee(Task.delay(close()).executeOn(kafkaScheduler))
    } else {
      obs
    }
  }

  /**
    * @return a task which will run any exec commands on our kafka scheduler
    */
  private def execNext() = {
    require(!closed, "RickKafkaConsumer is already closed")
    commandQueue.tryPoll.flatMap {
      case Some(exec) =>
        Task(exec.run(self)).executeOn(kafkaScheduler).map(_ => NoResults).void
      case _ => Task.unit
    }
  }

  def commitAsync(state: PartitionOffsetState): Future[Map[TopicPartition, OffsetAndMetadata]] = {
    val promise: Promise[Map[TopicPartition, OffsetAndMetadata]] = Promise[Map[TopicPartition, OffsetAndMetadata]]()

    if (state.nonEmpty) {
      object callback extends OffsetCommitCallback {
        override def onComplete(offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception): Unit = {
          logger.debug(s"commitAsync($offsets, $exception)")
          if (exception != null) {
            promise.tryFailure(exception)
          } else {
            promise.trySuccess(offsets.asScala.toMap)
          }
        }
      }
      logger.debug(s"commitAsync($state)")

      // Only commit offsets for partitions that we're currently assigned to.
      val assignedPartitions = {
        val consumerAssignments = assignments()
        val callbackAssignments = assignmentsAccordingToCallback()
        if (consumerAssignments != callbackAssignments) {
          logger.warn(s"!!! Consumer assignments $consumerAssignments disagrees with the callback assignments $callbackAssignments")
        }
        callbackAssignments
      }
      val offsetsToCommit = state.asTopicPartitionMap.view.filterKeys(assignedPartitions.contains).toMap

      consumer.commitAsync(offsetsToCommit.asJava, callback)
    } else {
      logger.trace(s"NOT committing empty state")
      promise.trySuccess(Map.empty)
    }
    promise.future
  }

  private def swallow(thunk: => Unit) = {
    Try(thunk).map(_ => true)
  }

  def seekToBeginningOnPartition(partition: Int, topics: Set[String] = defaultTopics) = swallow {
    logger.info(s"seekToBeginning(${partition}, $topics)")
    topics.foreach { topic =>
      val tp = new TopicPartition(topic, partition)
      consumer.seekToBeginning(java.util.Collections.singletonList(tp))
    }
  }

  def seekToBeginning(topics: Set[String] = defaultTopics) = swallow {
    logger.info(s"seekToBeginning($topics)")
    topics.foreach { topic =>
      val topicPartitions = assignmentPartitions(topics).map { partition =>
        new TopicPartition(topic, partition)
      }
      consumer.seekToBeginning(topicPartitions.asJava)
    }
  }
  def seekToEnd(topics: Set[String] = defaultTopics) = swallow {
    logger.info("seekToEndUnsafe")
    topics.foreach { topic =>
      val topicPartitions = assignmentPartitions(topics).map { partition =>
        new TopicPartition(topic, partition)
      }
      consumer.seekToEnd(topicPartitions.asJava)
    }
  }

  def assignToTopics(topics: Set[String] = defaultTopics): Try[Set[TopicPartition]] = {
    val pbt = partitionsByTopic()
    val allTopicPartitions = topics.flatMap { topic =>
      val topicPartitions = pbt.get(topic).map { partitions: List[KafkaPartitionInfo] =>
        partitions.map(_.asTopicPartition)
      }
      topicPartitions.getOrElse(Nil)
    }
    swallow(consumer.assign(allTopicPartitions.asJava)).map { _ =>
      allTopicPartitions
    }
  }

  def seekToOffset(offset: Long) = seekToCustom(_ => offset)

  def seekToCustom(computeOffset: KafkaPartitionInfo => Long) = swallow {

    val partitions = partitionsByTopic(true)
    partitions.collect {
      case (topic, partitions) =>
        partitions.foreach { pi: KafkaPartitionInfo =>
          val offset = computeOffset(pi)
          consumer.seek(new TopicPartition(topic, pi.partition), offset)
        }
    }
  }

  def seekTo(topicPartitionState: PartitionOffsetState, topics: Set[String] = defaultTopics) = swallow {
    logger.info(s"seekToUnsafe(${topicPartitionState})")
    for {
      topic               <- topics
      topicPartitions     <- topicPartitionState.offsetByPartitionByTopic.get(topic).toSeq
      (partition, offset) <- topicPartitions
    } yield {
      consumer.seek(new TopicPartition(topic, partition), offset)
    }
  }

  def positionsFor(partition: Int, topics: Set[String] = defaultTopics) = {
    val byTopic = topics.map { topic =>
      topic -> consumer.position(new TopicPartition(topic, partition))
    }
    byTopic.toMap
  }

  def committed(partition: Int, topics: Set[String] = defaultTopics): Map[String, OffsetAndMetadata] = {
    val byTopic = topics.map { topic =>
      topic -> consumer.committed(new TopicPartition(topic, partition))
    }
    byTopic.toMap
  }

  def assignmentPartitions(topics: Set[String] = defaultTopics): Set[Int] = {
    assignments().map { tp =>
      require(topics.contains(tp.topic()), s"consumer for topics $topics has assignment on ${tp.topic()}")
      tp.partition()
    }
  }

  def assignments(): Set[TopicPartition]                    = consumer.assignment().asScala.toSet
  def assignmentsAccordingToCallback(): Set[TopicPartition] = rebalanceListener.assignments

  def status(verbose: Boolean, topics: Set[String] = defaultTopics): String = {
    val byTopic = partitionsByTopic()

    val topicStatuses = topics.map { topic =>
      byTopic.get(topic).fold(s"topic '${topic}' doesn't exist") { partitions =>
        val ourAssignments = {
          val all = assignmentPartitions(topics)
          val detail = if (verbose) {
            val committedStatus = all.toSeq.map(i => committed(i, topics))
            committedStatus.mkString("\n\tCommit status:\n\t", "\n\t", "\n")
          } else {
            ""
          }
          all.mkString(s"assigned to ${all.size}: [", ",", s"]$detail")
        }

        s"'$topic' status (one of ${topics.size} topics [${topics.mkString("\n\t", "\n\t", "\n\t")}])\ncurrently $ourAssignments\n${TopicStatus(topic, partitions).toString}"
      }
    }
    topicStatuses.mkString("\n")
  }

  /**
    * @return a scala-friendly data structure containing the commit status of the kafka cluster
    */
  def committedStatus(topics: Set[String] = defaultTopics): List[CommittedStatus] = {
    val all: Set[Int] = assignmentPartitions(topics)
    partitionsByTopic().collect {
      case (topic, kafkaPartitions) =>
        val weAreSubscribed: Boolean = topics.contains(topic)

        val partitionStats: List[(KafkaPartitionInfo, Boolean)] = kafkaPartitions.map { kpi =>
          val isAssigned = all.contains(kpi.partition)
          (kpi, isAssigned)
        }

        val commitStats: mutable.Map[TopicPartition, OffsetAndMetadata] = consumer.committed(partitionStats.map(_._1.asTopicPartition).toSet.asJava).asScala

        val pos = PartitionOffsetState.fromKafka(commitStats.toMap)
        CommittedStatus(topic, weAreSubscribed, partitionStats, pos)
    }.toList
  }

  def isClosed() = closed

  override def close(): Unit = {
    withConsumer { c =>
      closed = true
      c.close()
      Schedulers.close(kafkaScheduler)
    }
  }

  override def withConsumer[A](withConsumer: RichKafkaConsumer[K, V] => A): Future[A] = {
    val cmd = ExecOnConsumer[K, V, A](withConsumer)
    import cats.syntax.apply._
    val task = commandQueue.offer(cmd) *> execNext

    task
      .runToFuture(asyncScheduler)
      .flatMap { _ =>
        cmd.promise.future
      }(asyncScheduler)
  }
}

object RichKafkaConsumer extends StrictLogging {

  def asJavaDuration(d: Duration): time.Duration = {
    if (d.isFinite) {
      java.time.Duration.ofMillis(d.toMillis)
    } else {
      java.time.Duration.ofDays(Long.MaxValue)
    }
  }

  private[consumer] def byteArrayValues(rootConfig: Config, kafkaScheduler: Scheduler, ioSched: Scheduler) = {
    val keyDeserializer   = new org.apache.kafka.common.serialization.StringDeserializer
    val valueDeserializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
    apply(rootConfig, keyDeserializer, valueDeserializer, kafkaScheduler)(ioSched)
  }

  private[consumer] def apply[K, V](rootConfig: Config,
                                    keyDeserializer: Deserializer[K],
                                    valueDeserializer: Deserializer[V],
                                    kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit ioSched: Scheduler): RichKafkaConsumer[K, V] = {
    val topics: Set[String] = kafka4m.consumerTopics(rootConfig)
    forConfig(rootConfig.getConfig("kafka4m.consumer"), keyDeserializer, valueDeserializer, topics, kafkaScheduler)
  }

  def forConfig[K, V](consumerConfig: Config,
                      keyDeserializer: Deserializer[K],
                      valueDeserializer: Deserializer[V],
                      topicOverrides: Set[String] = Set.empty,
                      kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit ioSched: Scheduler): RichKafkaConsumer[K, V] = {

    import args4c.implicits._
    val topics = if (topicOverrides.isEmpty) consumerConfig.asList("topic").toSet else topicOverrides

    val props: Properties = {
      val properties = kafka4m.util.Props.propertiesForConfig(consumerConfig)

      //
      // subscribe to our topic
      // .. properties.asScala.mkString()  is broken as it tries to cast things as strings, and some values are integers
      def propString = {
        val keys = properties.propertyNames.asScala
        keys
          .map { key =>
            s"$key : ${properties.getProperty(key.toString)}"
          }
          .mkString("\n\t", "\n\t", "\n\n")
      }

      logger.info(s"Creating consumer for '${topics.mkString(",")}', properties are:\n${propString}")
      properties
    }

    val consumer: KafkaConsumer[K, V] = new KafkaConsumer[K, V](props, keyDeserializer, valueDeserializer)
    val pollTimeout                   = consumerConfig.asDuration("pollTimeout")

    val capacity: BufferCapacity = consumerConfig.getInt("commandBufferCapacity") match {
      case n if n <= 0 => BufferCapacity.Unbounded()
      case n           => BufferCapacity.Bounded(n)
    }

    val queue = ConcurrentQueue.unsafe[Task, ExecOnConsumer[K, V, _]](capacity)

    val richConsumer = new RichKafkaConsumer(consumer, topics, pollTimeout, queue, kafkaScheduler, ioSched)

    if (consumerConfig.getBoolean("subscribeOnConnect")) {
      topics.foreach(t => richConsumer.subscribe(t))
    } else {
      logger.debug("subscribeOnConnect is false")
    }

    richConsumer
  }
}
