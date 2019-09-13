package kafka4m
package streams

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import args4c.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.producer.RichKafkaProducer
import kafka4m.util.{Env, Schedulers, Using}
import monix.eval.Task
import org.apache.kafka.clients.producer.RecordMetadata

import scala.concurrent.Future
import scala.concurrent.duration._

class StreamConsumerTest extends BaseKafka4mDockerSpec {

  override def testTimeout: FiniteDuration = 5.seconds

  "StreamConsumer" should {
    "close the stream when a single subset of it is consumed" in {
      val config = Kafka4mTest.configForTopic()
      Schedulers.using { s =>
        val Some(topic) = ensureTopicBlocking(config)(s)

        // create a record in our topic
        Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
          producer.send(topic, "hello", "world".getBytes).get(testTimeout.toMillis, TimeUnit.MILLISECONDS)

          Using(Env(config)) { env =>
            val setup: StreamConsumer.Setup = StreamConsumer(config)(env.io)

            val closed = new AtomicBoolean(false)
            val closeMe = Task.delay {
              closed.compareAndSet(false, true)
              setup.close()
            }
            val ("hello", bytes) = setup.output.guarantee(closeMe).headL.runToFuture(s).futureValue
            new String(bytes, "UTF-8") shouldBe "world"

            eventually {
              closed.get() shouldBe true
            }
          }
        }
      }
    }
    "consume streams" should {
      Schedulers.using { implicit sched =>
        val topic1 = s"topic1-${UUID.randomUUID}".filter(_.isLetterOrDigit)
        val topic2 = s"topic2-${UUID.randomUUID}".filter(_.isLetterOrDigit)
        val config: Config = {
          val c = ConfigFactory.parseString(s"""kafka4m.streams.topic=${topic1}
               |kafka4m.streams.application.id=${topic1}
               |kafka4m.producer.topic=${topic1}
            """.stripMargin)
          c.withFallback(ConfigFactory.load())
        }

        Given("A test topic, as kafka streams will fail if given a non-existent topic")
        val kafkaAdmin = RichKafkaAdmin(config)
        kafkaAdmin.createTopicSync(topic1, testTimeout)
        kafkaAdmin.createTopicSync(topic2, testTimeout)
        kafkaAdmin.topics().futureValue.keySet should contain allOf (topic1, topic2)

        When("We publishing to the new stream")
        val producer = RichKafkaProducer.byteArrayValues(config)

        val futures: Seq[Future[RecordMetadata]] = (0 to 100).flatMap { i =>
          val a = producer.sendAsync(topic1, s"first-$i", s"first value-$i".getBytes)
          val b = producer.sendAsync(topic2, s"second-$i", s"second value-$i".getBytes)
          List(a, b)
        }
        Future.sequence(futures).futureValue

        def verify(stream: StreamConsumer.Setup, expected: Seq[(String, String)]) = {
          val actual: List[(Key, Bytes)] = stream.output.take(expected.size).toListL.runSyncUnsafe(testTimeout)
          actual.size shouldBe expected.size
          actual.zip(expected).foreach {
            case ((actualKey, actualBytes), b) =>
              val actualPair = (actualKey, new String(actualBytes, "UTF-8"))
              actualPair shouldBe b
          }
        }

        def makeStream(topic: String): StreamConsumer.Setup = StreamConsumer(
          config.set("kafka4m.streams.topic", topic).set("kafka4m.streams.application.id", "test-{uniqueID}")
        )

        Using(makeStream(topic1)) { stream: StreamConsumer.Setup =>
          val expected: Seq[(String, String)] = (0 to 20).map { i =>
            (s"first-$i", s"first value-$i")
          }
          verify(stream, expected)
        }
        Using(makeStream(topic1)) { stream =>
          val expected = (0 to 20).map { i =>
            (s"first-$i", s"first value-$i")
          }
          verify(stream, expected)
        }
        Using(makeStream(topic2)) { stream =>
          val expected = (0 to 20).map { i =>
            (s"second-$i", s"second value-$i")
          }
          verify(stream, expected)
        }
      }
    }
  }
}
