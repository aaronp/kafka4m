package kafka4m.consumer

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import dockerenv.BaseKafkaSpec
import kafka4m.producer.RichKafkaProducer
import kafka4m.util.{Schedulers, Using}
import org.apache.kafka.clients.producer.RecordMetadata
import org.scalatest.concurrent.ScalaFutures

class KafkaConsumerFeedTest extends BaseKafkaSpec with ScalaFutures with StrictLogging {

  "KafkaConsumerFeed.unicast" should {
    "produce a stream of events" in {
      Schedulers.using { implicit sched =>
        val topic  = s"${getClass.getSimpleName}${System.currentTimeMillis}".filter(_.isLetterOrDigit)
        val config = KafkaConsumerFeedTest.testConfig(topic)
        Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
          val first: RecordMetadata = producer.sendAsync(topic, "foo", "first record".getBytes).futureValue
          first.offset() shouldBe 0L

          val second = producer.sendAsync(topic, "bar", "second record".getBytes).futureValue
          second.offset() shouldBe 1L

          Using(KafkaConsumerFeed(config)) { consumerFeed: KafkaConsumerFeed[String, Array[Byte]] =>
            val received = consumerFeed.unicast.take(2).toListL.runToFuture.futureValue
            received.head.offset() shouldBe 0
            received.tail.head.offset() shouldBe 1
          }
        }
      }
    }
  }
}

object KafkaConsumerFeedTest {
  def testConfig(topic: String): Config = ConfigFactory.parseString(s"""kafka4m.admin.topic : $topic
       |
       |kafka4m.consumer.topic : $topic
       |kafka4m.consumer.group.id : "test"$topic
       |kafka4m.consumer.application.id : "test"$topic
       |kafka4m.consumer.auto.offset.reset : earliest
       |
       |kafka4m.producer.topic : $topic
       |""".stripMargin).withFallback(ConfigFactory.load())
}
