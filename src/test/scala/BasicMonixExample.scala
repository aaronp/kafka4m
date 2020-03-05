import java.nio.charset.StandardCharsets

import args4c.implicits._
import com.typesafe.config.ConfigFactory
import kafka4m.producer.RichKafkaProducer
import kafka4m.util.Schedulers
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Consumer

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * An example of reading from kafka
  */
object BasicMonixExample extends App {

  dockerenv.kafka().start()

  val config = Array("kafka4m.topic=read-back").asConfig(ConfigFactory.load())

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
  val kafkaData = kafka4m.readRecords[String](config)

  //
  // and create another write which will consume key/values (strings and byte-arrays) into kafka
  //
  val kafkaWriter: Consumer[String, Long] = kafka4m.writeText(config)

  //
  // now wire it up -- attached the reader to the writer with some trivial transformation using some monix operations:
  //
  val task: Task[Long] = kafkaData
    .dump("from kafka")
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
