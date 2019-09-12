import com.typesafe.config.Config
import kafka4m.consumer.RichKafkaConsumer
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.streams.StreamConsumer
import kafka4m.util.Env
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

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
  def consumerObservable(config: Config): Observable[ConsumerRecord[Key, Bytes]] = {
    val env = Env(config)

    val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)(env.bounded)

    val topic = config.getString("kafka4m.consumer.topic")
    consumer.subscribe(topic)

    val closeMe = Task.delay {
      consumer.close()
      env.close()
    }
    consumer.asObservable.guarantee(closeMe)
  }

  /**
    * @param config the configuration which contains the kafka4m.streams config
    * @return A kafka data-stream based on the kafka streams API
    */
  def streamObservable(config: Config): Observable[KeyValue] = {
    val env                         = Env(config)
    val setup: StreamConsumer.Setup = StreamConsumer(config)(env.bounded)
    val closeMe = Task.delay {
      setup.close()
    }
    setup.output.guarantee(closeMe)
  }

  def publishConsumer[A: AsProducerRecord](config: Config): Consumer[A, Long] = {
    val apr                                  = AsProducerRecord[A]
    val rkp: RichKafkaProducer[apr.K, apr.V] = RichKafkaProducer[apr.K, apr.V](config, null, null)
    val fireAndForget                        = config.getBoolean("kafka4m.producer.fireAndForget")
    rkp.asConsumer(fireAndForget)(apr)
  }

}
