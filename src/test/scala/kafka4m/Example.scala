package kafka4m

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import kafka4m.consumer.RecordDecoder.ByteArrayDecoder
import kafka4m.consumer.{AckableRecord, ComputeResult, RecordDecoder}
import kafka4m.producer.AsProducerRecord
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * A basic example of writing data into and then load-balancing out-of Kafka
  */
object Example extends App with StrictLogging {
  val config = ConfigFactory.load()

  // let's ensure Kafka's running
  dockerenv.kafka().start()

  // our own case class
  case class SomeData(key: String, value: String)

  // a means of converting that case-class into a ProducerRecord
  val topic = kafka4m.producerTopic(config)
  implicit val asProducer = AsProducerRecord.liftForTopic(topic) { data: SomeData =>
    (data.key, data.value.getBytes())
  }

  // This gives us a monix Consumer of 'SomeData'
  val intoKafka: Consumer[SomeData, Long] = kafka4m.write[SomeData](config)

  // .. which we can plug into any monix Observable, e.g.
  val ourData: Observable[SomeData] = Observable.range(0, 10).map { i =>
    SomeData(s"key-$i", i.toString)
  }
  val writeToKafkaTask: Task[Long] = ourData.consumeWith(intoKafka)
  Await.result(writeToKafkaTask.runToFuture, 5.seconds)

  // and now with some data in our test-topic, we can consume it in a parallel way which is guaranteed NOT to commit
  // offsets back to Kafka until the tasks complete
  implicit val decoder: ByteArrayDecoder[SomeData] = RecordDecoder.forBytes { bytes =>
    val i = new String(bytes).toInt
    SomeData(s"key-$i", i.toString)
  }

  // here we write to our database, downstream API, whatever
  import cats.effect.concurrent.Ref
  val database: Ref[Task, Map[String, SomeData]] = Ref.unsafe[Task, Map[String, SomeData]](Map.empty[String, SomeData])

  val computeResults: Observable[ComputeResult[SomeData, Unit]] = kafka4m.loadBalance[SomeData, Unit]() { next: SomeData =>
    logger.info(s"Inserting: $next")
    database.update { byId =>
      byId.updated(next.key, next)
    }
  }

  val loadBalanceFuture = computeResults.foreach { compute: ComputeResult[SomeData, Unit] =>
    logger.info(s"Wrote: $compute")
  }

  logger.info("Waiting for out db writes...")
  val sizeT: Task[Int] = database.get.map(_.size).restartUntil(_ == 10)
  Await.result(sizeT.runToFuture, Duration.Inf)

  database.get.map(db => db.mkString(s"\nDB of size ${db.size}:\n", "\n", "\n")).foreach { mapStr =>
    logger.info(mapStr)
  }
}
