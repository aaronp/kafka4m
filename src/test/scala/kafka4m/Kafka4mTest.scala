package kafka4m

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.producer.AsProducerRecord
import kafka4m.util.{Props, Schedulers, Using}
import monix.eval.Task
import monix.reactive.Observable
import org.apache.kafka.clients.admin.TopicListing
import org.apache.kafka.clients.consumer.ConsumerRecord

class Kafka4mTest extends BaseKafka4mDockerSpec {

  "kafka4m.ensureTopicBlocking" should {
    "get or create topic" in {
      val config = Kafka4mTest.configForTopic()
      Schedulers.using { s =>
        val Some(topic) = kafka4m.ensureTopicBlocking(config)(s)
        Using(RichKafkaAdmin(config)) { admin =>
          val topics: Map[String, TopicListing] = admin.topics()(s).futureValue
          topics.keySet should contain(topic)
        }
      }
    }
  }
  "kafka4m" should {
    "be able to consume one topic into another" in {
      Given(s"A config with test topic")
      val config1 = Kafka4mTest.configForTopic()

      And("a publisher")
      val writer1 = kafka4m.writeText(config1)

      Schedulers.using { s =>

        val numberOfRecordsToWrite = 100
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val numWritten = testData.consumeWith(writer1).runToFuture(s).futureValue
        numWritten shouldBe numberOfRecordsToWrite

        And("map that data into another topic")
        val mappedData: Observable[String] = kafka4m.read(config1).take(numberOfRecordsToWrite).map { record =>
          val newNum = new String(record.value).toInt + 1000
          newNum.toString
        }

        Then("We should see the data in a new topic")
        val config2 = Kafka4mTest.configForTopic()
        val writer2 = kafka4m.writeText(config2)
        val readFromTwo = kafka4m.read(config2)
        mappedData.consumeWith(writer2).runToFuture(s).futureValue shouldBe numberOfRecordsToWrite
        val mappedAsList = readFromTwo.take(numberOfRecordsToWrite).toListL.runToFuture(s).futureValue
        val texts = mappedAsList.map { c =>
          val data = new String(c.value(), "UTF-8")
          data
        }

        texts.map(_.toInt) shouldBe (1000 until 1000 + numberOfRecordsToWrite).toList
      }
    }
  }

  "kafka4m.publisher" should {
    "publish data via kafka" in {
      Given(s"A config with test topic")
      val config = Kafka4mTest.configForTopic()

      And("a kafka consumer")
      val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = kafka4m.read(config)

      And("A kafka publisher")
      val writer = kafka4m.writeText(config)

      Schedulers.using { s =>
        val numberOfRecordsToWrite = 100
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val numWritten = testData.consumeWith(writer).runToFuture(s).futureValue
        numWritten shouldBe numberOfRecordsToWrite
      }
    }
  }
  "kafka4m.consumerObservable()" should {
    "read data via kafka" in {
      Schedulers.using { s =>
        Given("A config with a test topic")
        val config = Kafka4mTest.configForTopic()

        And("a kafka consumer")
        val numberOfRecordsToWrite = 100
        val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = kafka4m.read(config)
        val readBackFuture = kafkaData.take(numberOfRecordsToWrite).toListL

        And("A kafka publisher")
        val writer = kafka4m.writeText(config)

        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val done = testData.consumeWith(writer).runToFuture(s).futureValue
        done shouldBe numberOfRecordsToWrite

        Then("We should be able to read that data via the consumer observable")
        val readBack = readBackFuture.runToFuture(s).futureValue.map(_.offset())
        readBack.size shouldBe numberOfRecordsToWrite
        readBack shouldBe testData.map(_.toLong).toListL.runToFuture(s).futureValue
      }
    }
  }
}

object Kafka4mTest {

  def configForTopic(topic: String = UUID.randomUUID.toString.filter(_.isLetterOrDigit)): Config = {
    ConfigFactory.parseString(
      s"""kafka4m {
         |  consumer.topic : ${topic}
         |  consumer.group.id : ${topic}
         |  producer.topic : ${topic}
         |  streams.topic : ${topic}
         |}""".stripMargin).withFallback(ConfigFactory.load())
  }
}
