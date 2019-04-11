package kafkaquery.connect

import java.util
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

import args4c.RichConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.kafka.{PartitionData, PullLatestResponse}
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import org.apache.kafka.common.serialization.Deserializer

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * pimped KafkaConsumer
  *
  * @param client
  * @param defaultTimeout
  * @tparam K
  * @tparam V
  */
class RichKafkaConsumer[K, V](val client: KafkaConsumer[K, V], defaultTimeout: FiniteDuration) extends Iterable[ConsumerRecord[K, V]] with AutoCloseable with StrictLogging {

  private class Listener(topics: Set[String], offset: String) extends ConsumerRebalanceListener {
    import scala.collection.JavaConverters._
    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
      val list = partitions.asScala.toSeq
      logger.info(s"${list.size} partitions revoked: ${list.mkString(",")}")
    }

    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
      val list = partitions.asScala.toSeq
      logger.info(s"${list.size} partitions assigned: ${list.mkString(",")}")

      val filtered: Seq[TopicPartition] = list.filter(p => topics.contains(p.topic()))
      if (filtered.nonEmpty) {
        offset match {
          case "earliest" =>
            logger.info(s"Seeking to earliest for ${filtered}")
            client.seekToBeginning(filtered.asJava)
          case "latest" =>
            val ok = client.seekToEnd(filtered.asJava)
            logger.info(s"Seeking to latest for ${filtered}")
            client.seekToEnd(filtered.asJava)
          case number =>
            val offsetTry = Try(number.toLong)
            logger.info(s"Seeking to $offsetTry for ${filtered}")
            offsetTry.foreach { offset =>
              filtered.foreach { p =>
                client.seek(p, offset)
              }
            }
        }
      }
    }
  }

  def subscribe(topics: Set[String], fromOffset: Option[String]) = {
    import scala.collection.JavaConverters._
    fromOffset match {
      case None =>
        client.subscribe(topics.asJava)
      case Some(offset) =>
        client.subscribe(topics.asJava, new Listener(topics, offset))

    }
  }

  def pullLatest(topic: String, limit: Long, pollTimeout: FiniteDuration, timeout: FiniteDuration, formatKey: K => String): PullLatestResponse = {
    import scala.collection.JavaConverters._
    client.subscribe(List(topic).asJava)

    val found: Seq[ConsumerRecord[K, V]] = if (limit <= 0) {
      pullOrEmpty(pollTimeout).toSeq
    } else {
      val received  = ArrayBuffer[ConsumerRecord[K, V]]()
      var remaining = limit
      val deadline  = timeout.fromNow
      while (remaining > 0 && deadline.hasTimeLeft) {
        val next: Iterable[ConsumerRecord[K, V]] = pullOrEmpty(pollTimeout)
        val iter                                 = next.iterator
        while (remaining > 0 && iter.hasNext) {
          remaining = remaining - 1
          received += iter.next
        }
        iter.size
      }
      received
    }

    val results = found.map { record =>
      formatKey(record.key)
    }
    PullLatestResponse(topic, results)
  }

  def listTopics(): Map[String, List[PartitionData]] = {
    import scala.collection.JavaConverters._
    client.listTopics.asScala.mapValues { info: util.List[PartitionInfo] =>
      info.asScala.map { pi =>
        PartitionData(partition = pi.partition, leader = Option(pi.leader).fold("")(_.idString))
      }.toList
    }.toMap
  }

  def pull(timeout: FiniteDuration = defaultTimeout): Iterator[ConsumerRecord[K, V]] = {
    val all: Iterator[Iterable[ConsumerRecord[K, V]]] = Iterator.continually(pullOrEmpty(timeout))
    all.flatten
  }

  def pullOrEmpty(timeout: FiniteDuration): Iterable[ConsumerRecord[K, V]] = {
    import scala.collection.JavaConverters._
    val records: ConsumerRecords[K, V] = client.poll(java.time.Duration.ofMillis(timeout.toMillis))

    logger.info(s"got ${records.count} records")
    // we need to seek to the beginning -- which we can only do after the first poll
    if (records.count() > 0 && !isClosed()) {
      records.asScala
    } else {
      Nil
    }
  }

  private def isClosed() = {
    if (closed) {
      if (!cleanedUp) {
        cleanedUp = true
        logger.info("Actually closing kafka consumer")
        client.unsubscribe()
        client.close()
      }
      true
    } else {
      false
    }
  }

  @volatile private var closed    = false
  @volatile private var cleanedUp = false

  override def close(): Unit = {
    logger.info("Registering closing for kafka consumer")
    // kafka client isn't safe for multi-threaded access
    closed = true
  }

  override def iterator: Iterator[ConsumerRecord[K, V]] = pull()
}

object RichKafkaConsumer {

  object implicits {
    import concurrent.duration._
    implicit def asRichConsumer[K, V](client: KafkaConsumer[K, V]) = new RichKafkaConsumer[K, V](client, 5.seconds)
  }

  def apply[K, V](rootConfig: Config, keySerializer: Deserializer[K], valueSerializer: Deserializer[V]): RichKafkaConsumer[K, V] = {

    import args4c.implicits._
    val config: RichConfig = rootConfig
    val props              = propertiesForConfig(config.kafkaquery.consumer.config)

    val consumer = new KafkaConsumer[K, V](props, keySerializer, valueSerializer)
    implicits.asRichConsumer(consumer)
  }

  def byteArrayValues(rootConfig: Config): RichKafkaConsumer[String, Bytes] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def strings(rootConfig: Config): RichKafkaConsumer[String, String] = {
    val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    val valueSerializer = new org.apache.kafka.common.serialization.StringDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

}
