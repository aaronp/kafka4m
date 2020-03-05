package kafka4m.producer

import java.util.Properties
import java.util.concurrent.{Future => JFuture}

import cats.effect.{IO, Resource}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.util.Props
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler, Callback => MonixCallback}
import monix.reactive.Consumer
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.Serializer

import scala.concurrent.Future

final class RichKafkaProducer[K, V] private (val publisher: KafkaProducer[K, V]) extends AutoCloseable with StrictLogging {

  logger.info("Creating Producer")

  def sendAsync(kafkaTopic: String, key: K, value: V, callback: Callback = null, partition: Int = -1): Future[RecordMetadata] = {
    val promise = PromiseCallback()
    send(kafkaTopic, key, value, promise, partition)
    promise.future
  }

  /**
    * @param fireAndForget set to true if 'onNext' should send the value to the producer without waiting for the ack
    * @param ev
    * @tparam A the input type which will be converted to a ProducerRecord
    * @return a consumer which will consume 'A' values into Kafka and produce a number of inserted elements
    */
  def asConsumer[A](fireAndForget: Boolean, continueOnError: Boolean, closeOnComplete: Boolean)(implicit ev: AsProducerRecord.Aux[A, K, V]): Consumer[A, Long] = {
    val producer = this
    val consumer = Consumer.create[A, Long] {
      case (scheduler: Scheduler, cancelable: Cancelable, callback: MonixCallback[Throwable, Long]) =>
        new KafkaProducerObserver[A, K, V](ev, producer, scheduler, cancelable, callback, fireAndForget, continueOnError)
    }

    if (closeOnComplete) {
      consumer.mapTask { result =>
        Task {
          close()
          result
        }
      }
    } else consumer
  }

  def send(kafkaTopic: String, key: K, value: V, callback: Callback = null, partition: Int = -1): JFuture[RecordMetadata] = {
    val record = partition match {
      case n if n <= 0 => new ProducerRecord[K, V](kafkaTopic, key, value)
      case _           => new ProducerRecord[K, V](kafkaTopic, partition, key, value)
    }
    sendRecord(record, callback)
  }

  def sendRecord(record: ProducerRecord[K, V], callback: Callback = null): JFuture[RecordMetadata] = {
    logger.debug(s"Publishing $record")
    publisher.send(record, callback)
  }

  override def close(): Unit = {
    logger.info("Closing producer")
    publisher.close()
  }
}

object RichKafkaProducer extends StrictLogging {

  def asResource(rootConfig: Config): Resource[IO, RichKafkaProducer[String, Array[Byte]]] = {
    Resource.make(IO(byteArrayValues(rootConfig))) { pub =>
      IO(pub.close())
    }
  }

  def byteArrayValues(rootConfig: Config): RichKafkaProducer[String, Array[Byte]] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringSerializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArraySerializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def apply[K, V](rootConfig: Config, keySerializer: Serializer[K], valueSerializer: Serializer[V]): RichKafkaProducer[K, V] = {
    val props: Properties = Props.propertiesForConfig(rootConfig.getConfig("kafka4m.producer"))
    val publisher         = new KafkaProducer[K, V](props, keySerializer, valueSerializer)
    new RichKafkaProducer(publisher)
  }
}
