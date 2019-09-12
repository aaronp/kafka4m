package kafka4m.producer

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import dockerenv.BaseKafkaSpec
import kafka4m.util.Schedulers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class RichKafkaProducerTest extends BaseKafkaSpec with ScalaFutures with StrictLogging {
  override def testTimeout: FiniteDuration = 15.seconds

  "RichKafkaProducer.send" should {
    "send data through kafka" in {

      Schedulers.using { implicit sched =>
        val topic = s"topic-${UUID.randomUUID}".filter(_.isLetter)
        val config: Config = {
          val c = ConfigFactory.parseString(s"""kafka4m.streams.topic=$topic
               |kafka4m.streams.application.id=$topic
               |kafka4m.producer.topic=$topic
            """.stripMargin)
          c.withFallback(ConfigFactory.load())
        }

        val ingress = "someid payload".getBytes

        val producer             = RichKafkaProducer.byteArrayValues(config)
        val cb1: PromiseCallback = PromiseCallback()
        producer.send(topic, "foo", ingress, cb1)
        val cb2: PromiseCallback = PromiseCallback()
        producer.send(topic, "bar", ingress, cb2)

        val ack1 = cb1.future.futureValue
        ack1.offset.toInt should be >= 0

        val ack2 = cb2.future.futureValue
        ack2.partition shouldBe ack1.partition
        ack2.offset.toInt shouldBe ack1.offset.toInt + 1
      }
    }
  }
}
