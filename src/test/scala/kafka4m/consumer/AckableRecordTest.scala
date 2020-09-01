package kafka4m.consumer

import java.nio.ByteBuffer

import kafka4m.admin.ConsumerGroupStats
import kafka4m.data.PartitionOffsetState
import kafka4m.producer.AsProducerRecord
import kafka4m.util.Schedulers
import kafka4m.{AckBytes, BaseKafka4mDockerSpec, Kafka4mTestConfig}
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future
import scala.concurrent.duration._

class AckableRecordTest extends BaseKafka4mDockerSpec {

  "AckableRecord" should {
    "commit offsets when instructed" in {

      Schedulers.using { implicit sched =>
        Given("some AckableRecord records")
        val (topic, config)                      = Kafka4mTestConfig.next(closeOnComplete = false)
        val ackableRecords: Observable[AckBytes] = kafka4m.read(config)

        And("Some data in Kafka")
        implicit val asRecords = AsProducerRecord.liftForTopic[Long](topic) { i =>
          (i.toString, ByteBuffer.allocate(8).putLong(i).array())
        }
        val numRecords = 100
        val numWritten = Observable.range(0, numRecords).consumeWith(kafka4m.write[Long](config)).runToFuture.futureValue
        numWritten shouldBe numRecords

        // ensure we start w/ zero offsets
        val testAdmin = kafka4m.richAdmin(config)
        try {
          val beforeStatus: Seq[ConsumerGroupStats] = testAdmin.consumerGroupsStats.futureValue.map(_.forTopic(topic))
          beforeStatus.flatMap(_.offsetsByPartition.values).foreach(_ shouldBe 0L)

          When("we consume some messages then ask an ackable record to ack")
          var ackOffset: PartitionOffsetState                        = null
          val ackStream = ackableRecords.zipWithIndex.map {
            case (record, i) if i == numRecords / 2 =>
              And("The current state contains offsets for a partition we aren't currently assigned to")
              val state = record.offset.incOffsets().update(topic, 999, 999)
              whenReady(record.commit(state)) { committedOffsets =>
                Then("We should kermit offsets only for the currently assigned partition")
                committedOffsets.keys.size shouldBe 1
                val (topicPartition, offsetAndMetadata) = committedOffsets.head
                topicPartition.topic shouldBe topic
                topicPartition.partition shouldBe 0
                offsetAndMetadata.offset shouldBe 51
              }
              ackOffset = record.offset.incOffsets()
              record
            case (record: AckBytes, i) if i == numRecords - 1 =>
              record.withConsumer(_.close())
              record
            case (record, _) => record
          }

          // if we just stop consuming then we'll quit calling 'poll' and our ack will never complete :-(
          val shared              = ackStream.share
          val keepConsumingFuture = shared.countL.runToFuture

          val lastValue = shared.take(numRecords).delayOnNext(5.millis).lastL.runToFuture.futureValue

          And("we should see the offsets/partitions kermitted in Kafka")
          val readFromKafka: Seq[ConsumerGroupStats] = testAdmin.consumerGroupsStats.futureValue
          readFromKafka.flatMap(_.forTopic(topic).offsetsByPartition).toMap shouldBe Map(0 -> 51)
          lastValue.offset.offsetByPartitionByTopic(topic) shouldBe Map(0                  -> (numRecords - 1))
          ackOffset.offsetByPartitionByTopic(topic) shouldBe Map(0                         -> 51)

          And("Our commit ack should complete")
          keepConsumingFuture.cancel()
        } finally {
          testAdmin.close()
        }
      }
    }
  }
}
