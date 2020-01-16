import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.consumer.{AckableRecord, RichKafkaConsumer}
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
  def write[A](config: Config = ConfigFactory.load())(implicit ev: AsProducerRecord.Aux[A, Key, Bytes]): Consumer[A, Long] = {
    kafkaProducer(config).asConsumer(fireAndForget(config), continueOnError(config))(ev)
  }

  /**
    * @param config the kafka4m config
    * @tparam A
    * @return a RichKafkaProducer for the given config
    */
  def kafkaProducer[A, K, V](config: Config): RichKafkaProducer[K, V] = {
    RichKafkaProducer[K, V](config, null, null)
  }

  def byteArrayProducer[A](config: Config): RichKafkaProducer[Key, Bytes] = kafkaProducer[A, Key, Bytes](config)

  /**
    * @param config the kafka4m config
    * @param scheduler
    * @return A RichKafkaConsumer from the given kafka4m configuration
    */
  def kafkaConsumer(config: Config)(implicit scheduler: Scheduler): RichKafkaConsumer[String, Array[Byte]] = {
    val inst = RichKafkaConsumer.byteArrayValues(config)(scheduler)
    consumerTopics(config).foreach(t => inst.subscribe(t))
    inst
  }

  /**
    * @param config the kafka4m configuration which contains the 'kafka4m.consumer' values
    * @return an Observable of [[AckableRecord]]s from kafka. The offsets, etc will be controlled by the kafka4m.consumer configuration, which includes default offset strategy, etc.
    */
  def read(config: Config = ConfigFactory.load())(implicit scheduler: Scheduler): Observable[AckableRecord[ConsumerRecord[String, Array[Byte]]]] = {
    AckableRecord(closeOnComplete(config))(kafkaConsumer(config))
  }

  /**
    * @param config the kafka4m configuration which contains the 'kafka4m.consumer' values
    * @return an Observable of data coming from kafka. The offsets, etc will be controlled by the kafka4m.consumer configuration, which includes default offset strategy, etc.
    */
  def readRecords(config: Config = ConfigFactory.load())(implicit scheduler: Scheduler): Observable[ConsumerRecord[Key, Bytes]] = {
    read(config).map(_.record)
  }

  /** @param config the kafka4m config
    * @return true if observables should be closed when complete
    */
  def closeOnComplete(config: Config) = config.getBoolean("kafka4m.consumer.closeOnComplete")

  def fireAndForget(config: Config) = config.getBoolean("kafka4m.producer.fireAndForget")

  def continueOnError(config: Config) = config.getBoolean("kafka4m.producer.continueOnError")

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

  def richAdmin(config: Config) = RichKafkaAdmin(config)
}
