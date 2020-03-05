import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.consumer._
import kafka4m.producer.AsProducerRecord._
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.{FixedScheduler, Props}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.ExecutionContext

/**
  * The high-level API space for kafka consumers
  */
package object kafka4m {

  type AckRecord[K, A] = AckableRecord[ConsumerRecord[K, A]]
  type AckBytes        = AckRecord[String, Array[Byte]]

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
    kafkaProducer(config).asConsumer(fireAndForget(config), continueOnError(config), closeProducerOnComplete(config))(ev)
  }

  /**
    * @param config the kafka4m config
    * @return a RichKafkaProducer for the given config
    */
  def kafkaProducer[K, V](config: Config): RichKafkaProducer[K, V] = {
    RichKafkaProducer[K, V](config, null, null)
  }

  def byteArrayProducer(config: Config): RichKafkaProducer[Key, Bytes] = kafkaProducer[Key, Bytes](config)

  /**
    * load balance the given compute, returning a tuple of the kafka data together with the completed request/response data
    *
    * @param config
    * @param parallelism
    * @param kafkaScheduler
    * @param compute
    * @param scheduler
    * @tparam A
    * @tparam B
    * @return an observable of a data structure which contains the local offset (the single-threaded sequential order of the read message), the kafka record, and the deserialized request/response
    */
  def loadBalance[A: RecordDecoder.ByteArrayDecoder, B](
      config: Config = ConfigFactory.load(),
      parallelism: Int = Runtime.getRuntime.availableProcessors(),
      kafkaScheduler: Scheduler = FixedScheduler().scheduler)(compute: A => Task[B])(implicit scheduler: Scheduler): Observable[ComputeResult[A, B]] = {
    val task = stream(config, kafkaScheduler)(scheduler).map(_.stream.compute(parallelism)(compute))
    Observable.fromTask(task).flatten
  }

  /**
    *
    * @param config
    * @param kafkaScheduler
    * @param scheduler
    * @return a [[ConcurrentStream]] of the records
    */
  def stream(config: Config = ConfigFactory.load(), kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit scheduler: Scheduler): Task[KafkaStream[AckBytes]] = {
    KafkaStream(config, kafkaScheduler)(scheduler)
  }

  /**
    *
    * @param config the configuration
    * @param kafkaScheduler the single-threaded scheduler for when like, we have to talk to kafka
    * @return
    */
  def read[A: RecordDecoder.ByteArrayDecoder](config: Config = ConfigFactory.load(), kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit ioScheduler: Scheduler) = {
    val decoder = RecordDecoder.ByteArrayDecoder[A]
    readByteArray(config, kafkaScheduler)(ioScheduler).map(_.map(decoder.decode))
  }

  /**
    * @param config the kafka4m configuration which contains the 'kafka4m.consumer' values
    * @return an Observable of data coming from kafka. The offsets, etc will be controlled by the kafka4m.consumer configuration, which includes default offset strategy, etc.
    */
  def readRecords[A: RecordDecoder.ByteArrayDecoder](config: Config = ConfigFactory.load())(implicit scheduler: Scheduler): Observable[A] = {
    read(config).map(_.record)
  }

  def readByteArray(config: Config = ConfigFactory.load(), kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit ioScheduler: Scheduler): Observable[AckBytes] = {
    val csObs = Observable.fromTask(stream(config, kafkaScheduler)(ioScheduler))
    csObs.flatMap(_.stream.kafkaData)
  }

  /** @param config the kafka4m config
    * @return true if observables should be closed when complete
    */
  def closeConsumerOnComplete(config: Config) = config.getBoolean("kafka4m.consumer.closeOnComplete")

  def closeProducerOnComplete(config: Config) = config.getBoolean("kafka4m.producer.closeOnComplete")

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
  def consumerTopics(config: Config): Set[String] = Props.topics(config, "consumer")

  /** @param config the root configuration
    * @return the admin topic as per the config
    */
  def adminTopic(config: Config) = Props.topic(config, "admin")

  def richAdmin(config: Config) = RichKafkaAdmin(config)
}
