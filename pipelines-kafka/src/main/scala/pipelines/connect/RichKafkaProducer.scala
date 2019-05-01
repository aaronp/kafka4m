package pipelines.connect

import java.util.Properties

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.Serializer

/**
  * Supporting functions for publishing stuff
  *
  * @param scheduler
  * @tparam K
  * @tparam V
  */
class RichKafkaProducer[K, V](val publisher: KafkaProducer[K, V])(implicit scheduler: Scheduler) extends AutoCloseable with StrictLogging {
  def flush(): Unit = {
    publisher.flush()
  }

  def send(kafkaTopic: String, key: K, value: V): Unit = {
    val record = new ProducerRecord[K, V](kafkaTopic, key, value)
    publisher.send(record)
  }

  def sendTestMessage(topic: String, key: String, value: String) = {
    send(topic, key.asInstanceOf[K], value.asInstanceOf[V])
  }

  override def close(): Unit = {
    publisher.close()
  }
}

object RichKafkaProducer {

  def byteArrayValues(rootConfig: Config)(implicit scheduler: Scheduler): RichKafkaProducer[String, Bytes] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringSerializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArraySerializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def strings(rootConfig: Config)(implicit scheduler: Scheduler): RichKafkaProducer[String, String] = {
    val keySerializer   = new org.apache.kafka.common.serialization.StringSerializer
    val valueSerializer = new org.apache.kafka.common.serialization.StringSerializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def apply[K, V](rootConfig: Config, keySerializer: Serializer[K], valueSerializer: Serializer[V])(implicit scheduler: Scheduler): RichKafkaProducer[K, V] = {
    val props: Properties = propertiesForConfig(rootConfig.getConfig("pipelines.producer"))
    val publisher         = new KafkaProducer[K, V](props, keySerializer, valueSerializer)
    new RichKafkaProducer(publisher)
  }

}
