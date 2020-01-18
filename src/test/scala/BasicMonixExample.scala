import java.nio.charset.StandardCharsets

import args4c.implicits._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import kafka4m.consumer.AckableRecord
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.Schedulers
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * An example of reading from kafka
  */
object BasicMonixExample extends App {

  object commitExample extends StrictLogging {

    // our custom data type
    case class MyData(id : String, x: Int)

    // a means to unmarshall it from a record
    def unmarshal(bytes :Array[Byte]) : MyData = ???


    // some method to persist our data
    def writeToDB(value : MyData) : Future[Boolean] = ???

    kafka4m.read().map { ackable: AckableRecord[ConsumerRecord[String, Array[Byte]]] =>
      val data = unmarshal(ackable.record.value())
      val commitFuture: Future[Map[TopicPartition, OffsetAndMetadata]] = writeToDB(data).flatMap(_ => ackable.commitPosition())
      commitFuture.onComplete(x => logger.info("Committed: " + x))
      data
    }
  }
  dockerenv.kafka().start()

  val config = ConfigFactory.load()

  //
  // let's imperatively load a topic in kafka
  //
  val originalTopic = kafka4m.producerTopic(config)
  val producer      = RichKafkaProducer.byteArrayValues(config)

  val dataWritten = (0 to 10).map { i =>
    producer.send(originalTopic, s"key-$i", s"value-$i".getBytes(StandardCharsets.UTF_8))
  }

  //
  // now read data from kafka (assumes a configuration akin to kafka4m.consumer.topic = originalTopic)
  //
  val kafkaData = kafka4m.readRecords(config)

  //
  // and create another write which will consume key/values (strings and byte-arrays) into kafka
  //
  val kafkaWriter: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(Array("kafka4m.topic=read-back").asConfig(config))

  //
  // now wire it up -- attached the reader to the writer with some trivial transformation using some monix operations:
  //
  val task: Task[Long] = kafkaData
    .dump("from kafka")
    .map(r => (r.key.reverse, r.value))
    .take(dataWritten.size)
    .consumeWith(kafkaWriter)

  //
  // run it!
  //
  val numWritten = Schedulers.using { s =>
    Await.result(task.runToFuture(s), 1.minute)
  }
  println(s"Populated $numWritten records into Kafka 'read-back' topic from '$originalTopic'")
}
