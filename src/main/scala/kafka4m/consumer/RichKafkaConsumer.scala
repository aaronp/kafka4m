package kafka4m.consumer

import java.util.Properties
import java.{time, util}

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.{PartitionInfo, TopicPartition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

/**
  * A means of driving a kafka-stream using the consumer (not kafka streaming) API
  */
final class RichKafkaConsumer[K, V](val consumer: KafkaConsumer[K, V], val topics: Set[String], val defaultPollTimeout: Duration) extends AutoCloseable with StrictLogging {

  @volatile private var closed = false

  require(topics.nonEmpty, "empty topic set for consumer")
  require(topics.forall(_.nonEmpty), "blank topic set for consumer")

  private val javaPollDuration: time.Duration = RichKafkaConsumer.asJavaDuration(defaultPollTimeout)

  def partitionsByTopic(): Map[kafka4m.Key, List[PartitionInfo]] = {
    consumer.listTopics().asScala.mapValues(_.asScala.toList).toMap
  }

  def subscribe(topic: String, listener: ConsumerRebalanceListener = RebalanceListener): Unit = {
    logger.info(s"Subscribing to $topic")
    consumer.subscribe(java.util.Collections.singletonList(topic), listener)
  }

  def partitions: List[PartitionInfo] = {
    val byTopic = partitionsByTopic()
    topics.toList.flatMap(byTopic.getOrElse(_, Nil))
  }

  /**
    * @param closeOnComplete a flag indicating that this consumer should be closed when this observable is
    * @param numberOfMessagesToReceiveBetweenOffsetCommits the number of messages which will be received before we commit our offsets
    * @param scheduler
    * @return an observable which will commit the offsets every n messages
    */
  def asObservableCommitEvery(closeOnComplete: Boolean, numberOfMessagesToReceiveBetweenOffsetCommits: Int)(implicit scheduler: Scheduler): Observable[ConsumerRecord[K, V]] = {
    asObservableCommitEveryObservable(closeOnComplete, numberOfMessagesToReceiveBetweenOffsetCommits).map(_._3)
  }

  /**
    * @param closeOnComplete a flag indicating that this consumer should be closed when this observable is
    * @param numberOfMessagesToReceiveBetweenOffsetCommits the number of messages which will be received before we commit our offsets
    * @param scheduler
    * @return an observable which will commit the offsets every n messages
    */
  def asObservableCommitEveryObservable(closeOnComplete: Boolean, numberOfMessagesToReceiveBetweenOffsetCommits: Int)(implicit scheduler: Scheduler) = {

    val initialTuple = (PartitionOffsetState(), Option.empty[Future[Unit]], (null: ConsumerRecord[K, V]))

    asObservable(closeOnComplete).zipWithIndex.scan(initialTuple) {
      case ((state, _, _), (record, i)) if i % numberOfMessagesToReceiveBetweenOffsetCommits == 0 =>
        // gah! We have to let this run and not block so as not to FU Kafka's idea of liveliness for our consumer.
        val future = commitAsync(state.incOffsets()).map(_ => ())
        (PartitionOffsetState(), Option(future), record)
      case ((state, _, _), (record, _)) => (state.update(record), None, record)
    }
  }

  /**
    * Represent this consumer as an observable
    * @param closeOnComplete set to true if the underlying Kafka consumer should be closed when this observable completes
    */
  def asObservable(closeOnComplete: Boolean)(implicit scheduler: Scheduler): Observable[ConsumerRecord[K, V]] = {

    val obs: Observable[ConsumerRecord[K, V]] = repeatedObservable(poll())
    if (closeOnComplete) {
      obs.guarantee(Task.delay(close()))
    } else {
      obs
    }
  }

  /** @param state the state to commit
    * @return a future of the commit result
    */
  def commitAsync(state: PartitionOffsetState): Future[Map[TopicPartition, OffsetAndMetadata]] = {
    if (state.nonEmpty) {
      val promise = Promise[Map[TopicPartition, OffsetAndMetadata]]
      object callback extends OffsetCommitCallback {
        override def onComplete(offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception): Unit = {
          logger.info(s"commitAsync($offsets, $exception)")
          if (exception != null) {
            promise.tryFailure(exception)
          } else {
            promise.trySuccess(offsets.asScala.toMap)
          }
        }
      }
      logger.info(s"commitAsync($state)")
      consumer.commitAsync(state.asTopicPartitionMapJava, callback)
      promise.future
    } else {
      logger.info(s"NOT committing empty state")
      Future.successful(Map.empty)
    }
  }

  private def swallow(thunk: => Unit): Boolean = {
    try {
      thunk
      true
    } catch {
      case NonFatal(e) =>
        logger.error("" + e, e)
        false
    }
  }

  def seekToBeginning(partition: Int): Boolean = swallow {
    logger.info(s"seekToBeginning(${partition})")
    topics.foreach { topic =>
      val tp = new TopicPartition(topic, partition)
      consumer.seekToBeginning(java.util.Collections.singletonList(tp))
    }
  }

  def positionsFor(partition: Int) = {
    val byTopic = topics.map { topic =>
      topic -> consumer.position(new TopicPartition(topic, partition))
    }
    byTopic.toMap
  }

  def committed(partition: Int): Map[String, OffsetAndMetadata] = {
    val byTopic = topics.map { topic =>
      topic -> consumer.committed(new TopicPartition(topic, partition))
    }
    byTopic.toMap
  }

  /**
    * poll for records
    * @param timeout the poll timeout
    * @return the records returned from the poll
    */
  def poll(timeout: time.Duration = javaPollDuration): Iterable[ConsumerRecord[K, V]] = {
    try {
      val records: ConsumerRecords[K, V] = consumer.poll(timeout)
      logger.trace(s"Got ${records.count()} records from ${records.partitions().asScala.mkString(s"[", ",", "]")}")
      val forTopic: Iterable[ConsumerRecord[K, V]] = {
        records.asScala.filter { record =>
          topics.contains(record.topic())
        }
      }
      logger.trace(s"Got ${forTopic.size} of ${records.count()} for topic '$topics' records from ${records.partitions().asScala.mkString(s"[", ",", "]")}")
      forTopic
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Poll threw $e")
        throw e
    }
  }

  def assignmentPartitions: List[Int] = {
    consumer.assignment().asScala.toList.map { tp =>
      require(topics.contains(tp.topic()), s"consumer for topics $topics has assignment on ${tp.topic()}")
      tp.partition()
    }
  }

  def status(verbose: Boolean): String = {
    val byTopic: Map[kafka4m.Key, List[PartitionInfo]] = partitionsByTopic()

    val topicStatuses = topics.map { topic =>
      byTopic.get(topic).fold(s"topic '${topic}' doesn't exist") { partitions: Seq[PartitionInfo] =>
        val ourAssignments = {
          val all = assignmentPartitions
          val detail = if (verbose) {
            all.map(committed).mkString("\n\tCommit status:\n\t", "\n\t", "\n")
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

  def isClosed() = closed

  override def close(): Unit = {
    closed = true
    consumer.close()
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

  def byteArrayValues(rootConfig: Config)(implicit ioSched: Scheduler): RichKafkaConsumer[String, Array[Byte]] = {
    val keyDeserializer   = new org.apache.kafka.common.serialization.StringDeserializer
    val valueDeserializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
    apply(rootConfig, keyDeserializer, valueDeserializer)
  }

  def apply[K, V](rootConfig: Config, keyDeserializer: Deserializer[K], valueDeserializer: Deserializer[V])(implicit ioSched: Scheduler): RichKafkaConsumer[K, V] = {

    import args4c.implicits._
    val consumerConfig = rootConfig.getConfig("kafka4m.consumer")
    val topics         = kafka4m.consumerTopics(rootConfig)

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
    val pollTimeout                   = rootConfig.asDuration("kafka4m.consumer.pollTimeout")

    new RichKafkaConsumer(consumer, topics, pollTimeout)
  }
}
