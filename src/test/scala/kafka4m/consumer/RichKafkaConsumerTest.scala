package kafka4m.consumer

import kafka4m.admin.RichKafkaAdmin
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.{FixedScheduler, Schedulers, Using}
import kafka4m.{BaseKafka4mDockerSpec, Bytes, Kafka4mTestConfig}

import scala.util.Success

class RichKafkaConsumerTest extends BaseKafka4mDockerSpec {

  implicit object IntFromBytes extends BytesDecoder[Int] {
    override def decode(bytes: Bytes): Int = {
      new String(bytes).toInt
    }
  }

  "RichKafkaConsumer.status" should {
    "report current assignments and partitions" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next()
        Using(RichKafkaConsumer.byteArrayValues(config, FixedScheduler().scheduler, sched)) { consumer =>
          Using(RichKafkaAdmin(config))(_.createTopicSync(topic, testTimeout))

          val Seq(status) = consumer.committedStatus()
          status.topic shouldBe topic
          status.subscribed shouldBe true
          val List((partitionStats, false)) = status.partitionStats
          partitionStats.partition shouldBe 0
        }
      }
    }
  }
  "RichKafkaConsumer.assignmentPartitions" should {
    "return the assignmentPartitions" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next()
        Using(RichKafkaConsumer.byteArrayValues(config, FixedScheduler().scheduler, sched)) { consumer =>
          Using(RichKafkaAdmin(config))(_.createTopicSync(topic, testTimeout))
          consumer.assignmentPartitions shouldBe empty
        }
      }
    }
  }
  "RichKafkaConsumer.seekToBeginning" should {
    "seek to the beginning" in {
      Schedulers.using { implicit sched =>
        Given("Some messages in a topic")
        val (topic, config) = Kafka4mTestConfig.next()
        Using(RichKafkaConsumer.byteArrayValues(config, FixedScheduler().scheduler, sched)) { consumer =>
          Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
            val first  = producer.sendAsync(topic, "first", "value".getBytes(), partition = 0).futureValue
            val second = producer.sendAsync(topic, "second", "value".getBytes(), partition = 0).futureValue
            val third  = producer.sendAsync(topic, "third", "value".getBytes(), partition = 0).futureValue

            When("We subscribe and consume to the end")
            consumer.subscribe(topic, RebalanceListener)

            eventually {
              consumer.unsafePoll().toList.size shouldBe 3
            }

            And("seek to the beginning")
            eventually {
              consumer.seekToBeginning(0) shouldBe Success(true)
            }

            Then("we should see that offset as the first message")
            val readBack = eventually {
              consumer.unsafePoll().toList.head
            }
            readBack.key() shouldBe "first"
            readBack.offset() shouldBe first.offset()
          }
        }
      }
    }
  }

  def intAsRecord(topic: String) = AsProducerRecord.liftForTopic[Long](topic) { value =>
    //val bytes = ByteBuffer.allocate(8).putLong(value).array()
    (value.toString, value.toString.getBytes())
  }
}
