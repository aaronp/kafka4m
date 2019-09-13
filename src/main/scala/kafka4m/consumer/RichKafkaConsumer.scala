package kafka4m.consumer

import java.time
import java.util.Properties

import cats.effect.{IO, Resource}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.util.Props
import monix.execution.Scheduler
import monix.reactive.Observable
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.{PartitionInfo, TopicPartition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

/**
  * A means of driving a kafka-stream using the consumer (not kafka streaming) API
  */
class RichKafkaConsumer[K, V](consumer: KafkaConsumer[K, V], val topic: String, val defaultPollTimeout: Duration) extends AutoCloseable with StrictLogging {
  require(topic.nonEmpty, "empty topic set for consumer")

  private val javaPollDuration: time.Duration = RichKafkaConsumer.asJavaDuration(defaultPollTimeout)

  def partitionsByTopic(): Map[kafka4m.Key, List[PartitionInfo]] = {
    consumer.listTopics().asScala.mapValues(_.asScala.toList).toMap
  }

  def subscribe(topic: String, listener: ConsumerRebalanceListener = RebalanceListener): Unit = {
    logger.info(s"Subscribing to $topic")
    consumer.subscribe(java.util.Collections.singletonList(topic), listener)
  }

  def partitions: List[PartitionInfo] = partitionsByTopic().getOrElse(topic, Nil)

  def asObservable: Observable[ConsumerRecord[K, V]] = {
    val iterators: Observable[Iterable[ConsumerRecord[K, V]]] = Observable.repeatEval(poll())
    iterators.flatMap { iter: Iterable[ConsumerRecord[K, V]] =>
      Observable.fromIterable(iter)
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

  def seek(partition: Int, offset: Long): Boolean = swallow {
    logger.info(s"seek($partition, $offset)")
    val tp = new TopicPartition(topic, partition)
    consumer.seek(tp, offset)
  }

  def seekToBeginning(partition: Int): Boolean = swallow {
    logger.info(s"seekToBeginning(${partition})")
    val tp = new TopicPartition(topic, partition)
    consumer.seekToBeginning(java.util.Collections.singletonList(tp))
  }

  def seekToEnd(partition: Int): Boolean = swallow {
    logger.info(s"seekToEnd(${partition})")
    val tp = new TopicPartition(topic, partition)
    consumer.seekToEnd(java.util.Collections.singletonList(tp))
  }

  def positionFor(partition: Int): Long = consumer.position(new TopicPartition(topic, partition))

  def committed(partition: Int): OffsetAndMetadata = consumer.committed(new TopicPartition(topic, partition))

  def poll(timeout: time.Duration = javaPollDuration): Iterable[ConsumerRecord[K, V]] = {
    try {
      val records: ConsumerRecords[K, V] = consumer.poll(timeout)
      logger.trace(s"Got ${records.count()} records from ${records.partitions().asScala.mkString(s"[", ",", "]")}")
      val forTopic: Iterable[ConsumerRecord[K, V]] = records.records(topic).asScala
      logger.trace(s"Got ${forTopic.size} of ${records.count()} for topic '$topic' records from ${records.partitions().asScala.mkString(s"[", ",", "]")}")
      forTopic
    } catch {
      case NonFatal(e) =>
        logger.warn(s"Poll threw $e")
        Nil
    }
  }

  def assignmentPartitions: List[Int] = {
    consumer.assignment().asScala.toList.map { tp =>
      require(tp.topic() == topic, s"consumer for $topic has assignment on ${tp.topic()}")
      tp.partition()
    }
  }

  def status(verbose: Boolean): String = {
    val topics: Map[kafka4m.Key, List[PartitionInfo]] = partitionsByTopic()

    topics.get(topic).fold(s"topic '${topic}' doesn't exist") { partitions: Seq[PartitionInfo] =>
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

  override def close(): Unit = {
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

  def asResource(rootConfig: Config)(implicit ioSched: Scheduler): Resource[IO, RichKafkaConsumer[String, Array[Byte]]] = {
    Resource.make(IO(byteArrayValues(rootConfig))) { consumer =>
      IO(consumer.close())
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
    val topic          = Props.topic(rootConfig, "consumer")

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

      logger.info(s"Creating consumer for '$topic', properties are:\n${propString}")
      properties
    }

    val consumer: KafkaConsumer[K, V] = new KafkaConsumer[K, V](props, keyDeserializer, valueDeserializer)
    val pollTimeout                   = rootConfig.asDuration("kafka4m.consumer.pollTimeout")

    new RichKafkaConsumer(consumer, topic, pollTimeout)
  }
}
