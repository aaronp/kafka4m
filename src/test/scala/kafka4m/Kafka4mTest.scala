package kafka4m

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.admin.RichKafkaAdmin
import kafka4m.consumer.BytesDecoder
import kafka4m.util.{Schedulers, Using}
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.admin.TopicListing

import scala.concurrent.Future

class Kafka4mTest extends BaseKafka4mDockerSpec {

  "kafka4m.loadBalance" should {
    "provide a stream of the result values from in input" in {
      Schedulers.using { implicit s =>
        val (topic, config) = Kafka4mTestConfig.next()

        Using(kafka4m.byteArrayProducer(config)) { writer =>
          val expectedTotal = 20
          val futures = (0 until expectedTotal).map { i =>
            writer.sendAsync(topic, s"key$i", s"value$i".getBytes("UTF-8"))
          }
          Future.sequence(futures).futureValue.size shouldBe expectedTotal

          implicit val decoder: BytesDecoder[String] = BytesDecoder.lift { bytes =>
            new String(bytes, "UTF-8")
          }

          val kafkaData = kafka4m.loadBalance[String, String](config) { str: String =>
            Task.now(str)
          }
          val readBack = kafkaData.take(expectedTotal).toListL.runToFuture.futureValue
          readBack.size shouldBe expectedTotal
        }
      }
    }
  }
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
      val writer1: Consumer[String, Long] = kafka4m.writeText(config1)
      Schedulers.using { implicit s =>
        val numberOfRecordsToWrite = 100
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val numWritten                   = testData.consumeWith(writer1).runToFuture(s).futureValue
        numWritten shouldBe numberOfRecordsToWrite

        And("map that data into another topic")
        val reader = kafka4m.readRecords[String](config1)
        val mappedData: Observable[String] = reader.take(numberOfRecordsToWrite).map { record =>
          (record.toInt + 1000).toString
        }

        Then("We should see the data in a new topic")
        val config2 = Kafka4mTest.configForTopic()
        val writer2 = kafka4m.writeText(config2)

        mappedData.consumeWith(writer2).runToFuture(s).futureValue shouldBe numberOfRecordsToWrite
        val readFromTwo: Observable[String] = kafka4m.readRecords[String](config2)
        val texts                           = readFromTwo.take(numberOfRecordsToWrite).toListL.runToFuture(s).futureValue
        texts.map(_.toInt) shouldBe (1000 until 1000 + numberOfRecordsToWrite).toList
      }
    }
  }

  "kafka4m.publisher" should {
    "publish data via kafka" in {
      Schedulers.using { implicit s =>
        Given(s"A config with test topic")
        val config = Kafka4mTest.configForTopic()

        And("a kafka consumer")
        val kafkaData = kafka4m.read[String](config)
        And("A kafka publisher")
        val writer                 = kafka4m.writeText(config)
        val numberOfRecordsToWrite = 100
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val numWritten                   = testData.consumeWith(writer).runToFuture(s).futureValue
        numWritten shouldBe numberOfRecordsToWrite

        kafkaData.take(numberOfRecordsToWrite).toListL.runToFuture.futureValue.size shouldBe numberOfRecordsToWrite

      }
    }
  }
  "kafka4m.consumerObservable()" should {
    "read data via kafka" in {
      Schedulers.using { implicit s =>
        Given("A config with a test topic")
        val config = Kafka4mTest.configForTopic()

        And("a kafka consumer")
        val numberOfRecordsToWrite = 100
        val kafkaData              = kafka4m.readRecords[String](config)
        val readBackFuture         = kafkaData.take(numberOfRecordsToWrite).toListL

        And("A kafka publisher")
        val writer = kafka4m.writeText(config)
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        testData.consumeWith(writer).runToFuture(s).futureValue shouldBe numberOfRecordsToWrite

        Then("We should be able to read that data via the consumer observable")
        val readBack = readBackFuture.runToFuture(s).futureValue.map(_.toLong)
        readBack.size shouldBe numberOfRecordsToWrite
        readBack shouldBe testData.map(_.toLong).toListL.runToFuture(s).futureValue
      }
    }
  }
}

object Kafka4mTest {

  def configForTopic(topic: String = UUID.randomUUID.toString.filter(_.isLetterOrDigit)): Config = {
    ConfigFactory.parseString(s"""kafka4m {
         |  consumer.topic : ${topic}
         |  consumer.group.id : ${topic}
         |  producer.topic : ${topic}
         |  admin.topic : ${topic}
         |  streams.topic : ${topic}
         |}""".stripMargin).withFallback(ConfigFactory.load())
  }
}
