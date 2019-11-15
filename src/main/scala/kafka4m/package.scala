import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.consumer.RichKafkaConsumer
import kafka4m.producer.AsProducerRecord._
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.Props
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.ExecutionContext

/**
  * The high-level API space for kafka consumers
  */
package object kafka4m {

  type Key      = String
  type Bytes    = Array[Byte]
  type KeyValue = (Key, Bytes)

  /** @param config the kafka4m configuration
    * @return a consumer which will consume raw text data and write it with null keys
    */
  def writeText(config: Config = ConfigFactory.load()): Consumer[String, Long] = {
    write[String](config)(FromString(Props.topic(config, "producer")))
  }

  /** @param config the kafka4m configuration
    * @return a consumer which will consume a stream of key/byte-array values into kafka and return the number written
    */
  def writeKeyAndBytes(config: Config = ConfigFactory.load()): Consumer[(String, Array[Byte]), Long] = {
    write[(String, Array[Byte])](config)(FromKeyAndBytes(producerTopic(config)))
  }

  /** @param config the kafka4m configuration
    * @tparam A any type A which can be converted into a kafka ProducerRecord
    * @return a consumer of the 'A' values and produce the number written
    */
  def write[A](config: Config)(implicit ev: AsProducerRecord.Aux[A, Key, Bytes]): Consumer[A, Long] = {
    val fireAndForget = config.getBoolean("kafka4m.producer.fireAndForget")
    kafkaProducer(config).asConsumer(fireAndForget)(ev)
  }

  /**
    * @param config the kafka4m config
    * @tparam A
    * @return a RichKafkaProducer for the given config
    */
  def kafkaProducer[A, K, V](config: Config)(implicit apr: AsProducerRecord.Aux[A, K, V]) = {
    RichKafkaProducer[K, V](config, null, null)
  }

  /**
    * @param config the kafka4m config
    * @param scheduler
    * @return A RichKafkaConsumer from the given kafka4m configuration
    */
  def kafkaConsumer(config: Config)(implicit scheduler: Scheduler) = {
    val inst = RichKafkaConsumer.byteArrayValues(config)(scheduler)
    consumerTopics(config).foreach(t => inst.subscribe(t))
    inst
  }

  /**
    * @param config the kafka4m configuration which contains the 'kafka4m.consumer' values
    * @return an Observable of data coming from kafka. The offsets, etc will be controlled by the kafka4m.consumer configuration, which includes default offset strategy, etc.
    */
  def read(config: Config)(implicit scheduler: Scheduler): Observable[ConsumerRecord[Key, Bytes]] = {
    val closeOnComplete = config.getBoolean("kafka4m.consumer.closeOnComplete")
    kafkaConsumer(config).asObservable(closeOnComplete)
  }

  /**
    * Kafka Streams will fail if the topic does not yet exist. This way we can provide a means to 'getOrCreate' a topic
    * if that's how it's configured.
    *
    * @param config
    * @param ec
    * @return
    */
  def ensureTopicBlocking(config: Config)(implicit ec: ExecutionContext): Option[String] = {
    RichKafkaAdmin.ensureTopicBlocking(config)
  }

  /** @param config the root configuration
    * @return the producer topic as per the config
    */
  def producerTopic(config: Config) = Props.topic(config, "producer")

  /** @param config the root configuration
    * @return the consumer topic as per the config
    */
  def consumerTopics(config: Config) = Props.topics(config, "consumer")

  /** @param config the root configuration
    * @return the admin topic as per the config
    */
  def adminTopic(config: Config) = Props.topic(config, "admin")
}
