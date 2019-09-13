package kafka4m.consumer

import java.util.concurrent.atomic.AtomicLong

import kafka4m.BaseKafka4mDockerSpec
import kafka4m.admin.RichKafkaAdmin
import kafka4m.producer.RichKafkaProducer
import kafka4m.util.Schedulers

import scala.concurrent.Future

class RichKafkaConsumerTest extends BaseKafka4mDockerSpec {

  private val id = new AtomicLong(System.currentTimeMillis)

  private def nextTopic() = {
    s"${getClass.getSimpleName}${id.incrementAndGet()}".filter(_.isLetterOrDigit)
  }

  "RichKafkaConsumer.status" should {
    "report current assignments and partitions" in {
      Schedulers.using { implicit sched =>
        val topic                                            = nextTopic()
        val config                                           = KafkaConsumerFeedTest.testConfig(topic)
        val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)

        RichKafkaAdmin(config).createTopicSync(topic, testTimeout)

        val statusLines = consumer.status(true).linesIterator.toList
        statusLines should contain("currently assigned to 0: []")
        statusLines.map(_.trim) should contain(s"Partition(topic = $topic, partition = 0, leader = 0, replicas = [0], isr = [0], offlineReplicas = [])")
      }
    }
  }
  "RichKafkaConsumer.assignmentPartitions" should {
    "return the assignmentPartitions" in {
      Schedulers.using { implicit sched =>
        val topic                                            = nextTopic()
        val config                                           = KafkaConsumerFeedTest.testConfig(topic)
        val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)

        RichKafkaAdmin(config).createTopicSync(topic, testTimeout)

        consumer.assignmentPartitions shouldBe empty
      }
    }
  }
  "RichKafkaConsumer.seekToBeginning" should {
    "seek to the beginning" in {
      Schedulers.using { implicit sched =>
        Given("Some messages in a topic")
        val topic                                            = nextTopic()
        val config                                           = KafkaConsumerFeedTest.testConfig(topic)
        val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)

        val producer = RichKafkaProducer.byteArrayValues(config)
        val first    = producer.sendAsync(topic, "first", "value".getBytes(), partition = 0).futureValue
        val second   = producer.sendAsync(topic, "second", "value".getBytes(), partition = 0).futureValue
        val third    = producer.sendAsync(topic, "third", "value".getBytes(), partition = 0).futureValue

        When("We subscribe and consume to the end")
        consumer.subscribe(topic, RebalanceListener)

        eventually {
          consumer.poll().toList.size shouldBe 3
        }

        And("seek to the beginning")
        eventually {
          consumer.seekToBeginning(0) shouldBe true
        }

        Then("we should see that offset as the first message")
        val readBack = eventually {
          consumer.poll().toList.head
        }
        readBack.key() shouldBe "first"
        readBack.offset() shouldBe first.offset()
      }
    }
  }
  "RichKafkaConsumer.seekToEnd" ignore {
    "seek to the end" in {
      Schedulers.using { implicit sched =>
        Given("Some messages in a topic")
        val topic                                            = nextTopic()
        val config                                           = KafkaConsumerFeedTest.testConfig(topic)
        val consumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config)

        val producer = RichKafkaProducer.byteArrayValues(config)
        val first    = producer.sendAsync(topic, "first", "value".getBytes(), partition = 0).futureValue
        val second   = producer.sendAsync(topic, "second", "value".getBytes(), partition = 0).futureValue
        val third    = producer.sendAsync(topic, "third", "value".getBytes(), partition = 0).futureValue

        When("We subscribe")
        consumer.subscribe(topic, RebalanceListener)
        eventually {
          consumer.poll().size shouldBe 3
        }

        And("add a bunch more messages")
        val futures = (0 to 100).map { i =>
          producer.sendAsync(topic, s"msg-$i", "value".getBytes(), partition = 0)
        }
        Future.sequence(futures).futureValue

        And("seek to the end")
        eventually {
          consumer.seekToEnd(0) shouldBe true
        }

        val last = producer.sendAsync(topic, "last", "value".getBytes(), partition = 0).futureValue

        Then("we should see the last message")
        val readBack = eventually {
          consumer.poll().toList.head
        }
        readBack.key() shouldBe "last"
        readBack.offset() shouldBe last.offset()
      }
    }
  }
}
