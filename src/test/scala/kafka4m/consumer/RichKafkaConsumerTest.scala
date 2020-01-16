package kafka4m.consumer

import java.nio.ByteBuffer

import kafka4m.admin.RichKafkaAdmin
import kafka4m.producer.{AsProducerRecord, RichKafkaProducer}
import kafka4m.util.{Schedulers, Using}
import kafka4m.{BaseKafka4mDockerSpec, Kafka4mTestConfig, kafkaConsumer}
import monix.reactive.Observable

import scala.concurrent.Future

class RichKafkaConsumerTest extends BaseKafka4mDockerSpec {

  "RichKafkaConsumer.status" should {
    "report current assignments and partitions" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next()
        Using(RichKafkaConsumer.byteArrayValues(config)) { consumer =>
          Using(RichKafkaAdmin(config))(_.createTopicSync(topic, testTimeout))

          val statusLines = consumer.status(true).linesIterator.toList
          statusLines should contain("currently assigned to 0: []")
          statusLines.map(_.trim) should contain(s"Partition(topic = $topic, partition = 0, leader = 0, replicas = [0], isr = [0], offlineReplicas = [])")
        }
      }
    }
  }
  "RichKafkaConsumer.assignmentPartitions" should {
    "return the assignmentPartitions" in {
      Schedulers.using { implicit sched =>
        val (topic, config) = Kafka4mTestConfig.next()
        Using(RichKafkaConsumer.byteArrayValues(config)) { consumer =>
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
        Using(RichKafkaConsumer.byteArrayValues(config)) { consumer =>
          Using(RichKafkaProducer.byteArrayValues(config)) { producer =>
            val first  = producer.sendAsync(topic, "first", "value".getBytes(), partition = 0).futureValue
            val second = producer.sendAsync(topic, "second", "value".getBytes(), partition = 0).futureValue
            val third  = producer.sendAsync(topic, "third", "value".getBytes(), partition = 0).futureValue

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
    }
  }
  "RichKafkaConsumer.asObservable" should {
    "periodically kermit offsets" in {
      val (topic, config) = Kafka4mTestConfig.next()
      implicit val ev     = intAsRecord(topic)
      Schedulers.using { implicit io =>
        Given("A running kafka")
        val writer     = kafka4m.write[Long](config)
        val numRecords = 10000
        val numWritten = Observable.range(0, numRecords).consumeWith(writer).runToFuture.futureValue
        numWritten shouldBe numRecords

        val admin    = kafka4m.richAdmin(config)
        val consumer = kafka4m.kafkaConsumer(config)
        try {
          val read = consumer.asObservableCommitEveryObservable(true, 5).take(100).toListL.runToFuture.futureValue
          read.size shouldBe 100

          val Seq(firstStatus) = admin.consumerGroupsStats.futureValue.filter(_.groupId == s"test${topic}")
          firstStatus.offsetsByPartition should not be (empty)
          firstStatus.offsetsByPartition shouldBe Map(0 -> 95)
        } finally {
          admin.close()
          consumer.close()
        }
      }
    }

    "honour the default auto.commit : false and explicit calls to commitAsync" in {
      val (topic, config) = Kafka4mTestConfig.next()
      implicit val ev     = intAsRecord(topic)
      Schedulers.using { implicit io =>
        Given("Some data in kafka")
        val writer     = kafka4m.write[Long](config)
        val numRecords = 10000
        val numWritten = Observable.range(0, numRecords).consumeWith(writer).runToFuture.futureValue
        numWritten shouldBe numRecords
        val admin = kafka4m.richAdmin(config)

        case class Received(state: PartitionOffsetState, offset: Long, value: Long)
        def take(n: Int, kermitOffsets: Boolean): Seq[Received] = {
          Using(kafkaConsumer(config)) { c =>
            val obs = c.asObservableCommitEveryObservable(!kermitOffsets, 10)

            val list = obs.take(n).toListL.runToFuture.futureValue

            c.isClosed() shouldBe !kermitOffsets

            val state = list.last._1

            if (kermitOffsets) {
              val f    = c.commitAsync(state.incOffsets)
              var done = false

              // this is nasty -- the 'commitAsync' fails if we don't keep polling while the commit request is processed,
              // as kafka needs to maintain its liveliness status!
              Future {
                while (!done) {
                  c.poll()
                  Thread.sleep(100)
                }
              }

              f.futureValue
              done = true
            }
            list.map {
              case (state, _, record) =>
                Received(state, record.offset(), ByteBuffer.wrap(record.value()).getLong())
            }
          }
        }

        def assertFirstThree(list: Seq[Received]) = {
          list.map(_.value) shouldBe List(0, 1, 2)
          list.map(_.offset) shouldBe List(0, 1, 2)
          val topics = list.flatMap(_.state.offsetByPartitionByTopic.keys)
          topics should contain only (topic)
          val firstOffsets = list.flatMap(_.state.offsetByPartitionByTopic.get(topic))
          firstOffsets shouldBe List(Map(0 -> 1L), Map(0 -> 2L))
        }
        And("We create a consumer which reads the first three messages")
        val firstThree = take(3, kermitOffsets = false)
        Then("We should see our first three messages with expected offsets/partitions")
        assertFirstThree(firstThree)

        withClue("we should not be automatically committing offsets, and have not yet explicitly committed anything") {
          val Seq(firstStatus) = admin.consumerGroupsStats.futureValue.filter(_.groupId == s"test${topic}")
          firstStatus.offsetsByPartition should be(empty)
        }

        When("We connect again and take some messages")
        val secondTen: Seq[Received] = take(10, kermitOffsets = true)

        Then("We should have received the first three again BS explicitly commit our state")
        assertFirstThree(secondTen.take(3))

        withClue("Our offset should now be kermitted") {
          eventually {
            val Seq(firstStatus) = admin.consumerGroupsStats.futureValue.filter(_.groupId == s"test${topic}")
            firstStatus.groupId shouldBe ("test" + topic)
            firstStatus.offsetsByPartition should be(secondTen.last.state.incOffsets().offsetByPartitionByTopic(topic))
          }
        }

        And("Take the next record after a commit should return the next record")
        val Seq(nextRecordAfterCommit) = take(1, kermitOffsets = false)

        Then("it really should be the next record after our committed offset")
        nextRecordAfterCommit.value shouldBe 10
        nextRecordAfterCommit.offset shouldBe 10

        admin.close()
      }
    }
  }

  def intAsRecord(topic: String) = AsProducerRecord.liftForTopic[Long](topic) { value =>
    val bytes = ByteBuffer.allocate(8).putLong(value).array()
    (value.toString, bytes)
  }
}
