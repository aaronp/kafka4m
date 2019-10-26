package kafka4m.producer

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.BaseKafka4mDockerSpec
import kafka4m.util.{Schedulers, Using}

class RichKafkaProducerTest extends BaseKafka4mDockerSpec {

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

        Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
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
}
