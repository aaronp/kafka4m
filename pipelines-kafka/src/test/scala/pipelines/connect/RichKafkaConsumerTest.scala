package pipelines.connect

import com.typesafe.config.{Config, ConfigFactory}
import monix.execution.Scheduler
import pipelines.Using
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.collection.JavaConverters._

/**
  * The 'main' test -- mixing in other test traits gives us the ability to have a 'before/after all' step which can
  * apply to ALL our tests, and so we don't stop/start e.g. kafka container for each test suite
  */
class RichKafkaConsumerTest extends BaseDockerSpec("scripts/kafka") {

  "RichKafkaConsumer" should {
    "consume kafka messages" in {
      implicit val sched = Scheduler.io("RichKafkaConsumerTest")

      try {
        val config = ConnectMain.defaultConfig()
        Using(RichKafkaProducer.strings(config)) { producer =>
          producer.send("foo", "testkey", "test")
          producer.send("bar", "testkey", "test")
          producer.send("example", "testkey", "test")

          Using(RichKafkaConsumer.strings(config)) { consumer: RichKafkaConsumer[String, String] =>
            eventually {
              val topics = consumer.listTopics
              topics.keySet should contain only ("foo", "bar", "example")
            }
          }
        }
      } finally {
        sched.shutdown()
      }
    }
  }

}

object RichKafkaConsumerTest {

  def newKafkaConsumer(rootConfig: Config, topics: Set[String]): KafkaConsumer[String, Bytes] = {
    val config = {
      ConfigFactory.parseString(s"""bootstrap.servers : "localhost:9092"
                                   |group.id : "${BaseDockerSpec.randomString()}"
                                   |client.id: "${BaseDockerSpec.randomString()}"
                                   |auto.offset.reset : "earliest"
        """.stripMargin).withFallback(rootConfig.getConfig("akka.kafka.consumer"))
    }
    val properties = propertiesForConfig(config)
    val consumer   = new KafkaConsumer[String, Bytes](properties)
    consumer.subscribe(topics.asJava)
    consumer
  }

}
