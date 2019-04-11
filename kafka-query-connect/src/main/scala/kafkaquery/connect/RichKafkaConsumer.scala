package kafkaquery.connect

import java.util.concurrent.ScheduledExecutorService

import args4c.RichConfig
import com.typesafe.config.Config
import kafkaquery.kafka.{PartitionData, PullLatestResponse}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

class RichKafkaConsumer[K, V](val client: KafkaConsumer[K, V], defaultTimeout: FiniteDuration) extends AutoCloseable {

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
    client.listTopics.asScala.mapValues { info =>
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
    // we need to seek to the beginning -- which we can only do after the first poll
    if (records.count() > 0) {
      records.asScala
    } else {
      Nil
    }
  }

  override def close(): Unit = {
    client.close()
  }
}

object RichKafkaConsumer {

  object implicits {
    import concurrent.duration._
    implicit def asRichConsumer[K, V](client: KafkaConsumer[K, V]) = new RichKafkaConsumer[K, V](client, 5.seconds)
  }

  def apply[K, V](rootConfig: Config, keySerializer: Deserializer[K], valueSerializer: Deserializer[V])(implicit scheduler: ScheduledExecutorService): RichKafkaConsumer[K, V] = {

    import args4c.implicits._
    val config: RichConfig = rootConfig
    val props              = propertiesForConfig(config.kafkaquery.consumer.config)

    val consumer = new KafkaConsumer[K, V](props, keySerializer, valueSerializer)
    implicits.asRichConsumer(consumer)
  }

  def byteArrayValues(rootConfig: Config)(implicit scheduler: ScheduledExecutorService): RichKafkaConsumer[String, Bytes] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def strings(rootConfig: Config)(implicit scheduler: ScheduledExecutorService): RichKafkaConsumer[String, String] = {
    val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    val valueSerializer = new org.apache.kafka.common.serialization.StringDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

}
