import com.typesafe.config.Config
import kafka4m.admin.RichKafkaAdmin
import kafka4m.consumer.RichKafkaConsumer
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.{Env, Props}
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord
import kafka4m.producer.AsProducerRecord._
import scala.concurrent.ExecutionContext

/**
  * The high-level API space for kafka consumers
  */
package object kafka4m {

  type Key      = String
  type Bytes    = Array[Byte]
  type KeyValue = (Key, Bytes)

  /**
    * @param config the kafka4m configuration which contains the 'kafka4m.consumer' values
    * @return an Observable of data coming from kafka. The offsets, etc will be controlled by the kafka4m.consumer configuration, which includes default offset strategy, etc.
    */
  def read(config: Config): Observable[ConsumerRecord[Key, Bytes]] = {
    val env = Env(config)

    val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)(env.io)

    val topic = Props.topic(config, "consumer")
    consumer.subscribe(topic)

    val closeMe = Task.delay {
      consumer.close()
      env.close()
    }
    consumer.asObservable.guarantee(closeMe)
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

  def writeText(config: Config): Consumer[String, Long] = write[String](config)(FromString(Props.topic(config, "producer")))

  def writeBytes(config: Config): Consumer[Array[Byte], Long] = write[Array[Byte]](config)(FromBytes(Props.topic(config, "producer")))

  def write[A: AsProducerRecord](config: Config): Consumer[A, Long] = {
    val apr                                  = AsProducerRecord[A]
    val rkp: RichKafkaProducer[apr.K, apr.V] = RichKafkaProducer[apr.K, apr.V](config, null, null)
    val fireAndForget                        = config.getBoolean("kafka4m.producer.fireAndForget")
    rkp.asConsumer(fireAndForget)(apr)
  }

}
