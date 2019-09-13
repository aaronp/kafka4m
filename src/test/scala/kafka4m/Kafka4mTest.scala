package kafka4m

import java.util.UUID

import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.producer.AsProducerRecord
import kafka4m.util.Schedulers
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

class Kafka4mTest extends BaseKafka4mDockerSpec {

  "kafka4m.publisher" should {
    "publish data via kafka" in {
      Given(s"A config with test topic")
      val config = Kafka4mTest.configForTopic()

      And("a kafka consumer")
      val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = kafka4m.consumerObservable(config)

      And("A kafka publisher")
      val writer: Consumer[String, Long] = {
        implicit def asRecord: AsProducerRecord[String] = AsProducerRecord.FromString(config.getString("kafka4m.producer.topic"))

        kafka4m.publishConsumer[String](config)
      }

      Schedulers.using { s =>
        val numberOfRecordsToWrite = 100
        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val numWritten                   = testData.consumeWith(writer).runToFuture(s).futureValue
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
        val numberOfRecordsToWrite                            = 100
        val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = kafka4m.consumerObservable(config)
        val readBackFuture                                    = kafkaData.take(numberOfRecordsToWrite).toListL

        And("A kafka publisher")
        val writer: Consumer[String, Long] = {
          implicit def asRecord: AsProducerRecord[String] = AsProducerRecord.FromString(config.getString("kafka4m.producer.topic"))

          kafka4m.publishConsumer[String](config)
        }

        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val done                         = testData.consumeWith(writer).runToFuture(s).futureValue
        done shouldBe numberOfRecordsToWrite

        Then("We should be able to read that data via the consumer observable")
        val readBack = readBackFuture.runToFuture(s).futureValue.map(_.offset())
        readBack.size shouldBe numberOfRecordsToWrite
        readBack shouldBe testData.map(_.toLong).toListL.runToFuture(s).futureValue
      }
    }
  }
  "kafka4m.streamObservable" should {
    "read data via kafka" in {
      Schedulers.using { s =>
        Given("A config with a test topic")
        val config = Kafka4mTest.configForTopic()

        And("a kafka streamObservable")
        val numberOfRecordsToWrite              = 100
        val kafkaData: Observable[(Key, Bytes)] = kafka4m.streamObservable(config)
        val readBackFuture                      = kafkaData.take(numberOfRecordsToWrite).toListL

        And("A kafka publisher")
        val writer: Consumer[String, Long] = {
          implicit def asRecord: AsProducerRecord[String] = AsProducerRecord.FromString(config.getString("kafka4m.producer.topic"))

          kafka4m.publishConsumer[String](config)
        }

        When("We push some test data through the publisher")
        val testData: Observable[String] = Observable.fromIterator(Task.eval(Iterator.from(0))).map(_.toString).take(numberOfRecordsToWrite)
        val done                         = testData.consumeWith(writer).runToFuture(s).futureValue
        done shouldBe numberOfRecordsToWrite

        Then("We should be able to read that data via the consumer observable")
        val readBack = readBackFuture.runToFuture(s).futureValue.map(_._1)
        readBack.size shouldBe numberOfRecordsToWrite
        readBack shouldBe testData.toListL.runToFuture(s).futureValue
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
         |  streams.topic : ${topic}
         |}""".stripMargin).withFallback(ConfigFactory.load())
  }
}
