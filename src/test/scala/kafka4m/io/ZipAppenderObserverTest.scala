package kafka4m.io

import java.time.ZonedDateTime

import kafka4m.{BaseKafka4mSpec, Bytes, Key}
import kafka4m.partitions.{FlushBucket, ForceFlushBuckets, MiniBatchState, PartitionEvent, TimeBucket}
import kafka4m.util.Schedulers
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.record.TimestampType

import scala.concurrent.duration._

class ZipAppenderObserverTest extends BaseKafka4mSpec {

  "ZipAppenderObserver" should {
    "append data to zip entries" in {
      Schedulers.using { implicit s =>
        withTmpDir { dir =>
          val input = Observable.fromIterable((0 to 240 by 30).map { time =>
            new ConsumerRecord[Key, Bytes](
              "topic",
              0,
              0,
              time,
              TimestampType.CREATE_TIME,
              ConsumerRecord.NULL_CHECKSUM,
              ConsumerRecord.NULL_SIZE,
              ConsumerRecord.NULL_SIZE,
              s"key$time",
              s"$time".getBytes
            )
          })

          val appendEvents: Observable[PartitionEvent[ConsumerRecord[Key, Bytes], TimeBucket]] = {
            // let's group into 1 minute buckets
            val bucketMinutes = 1
            MiniBatchState.byTime(input, 2, bucketMinutes.minutes)
          }
        // :+ ForceFlushBuckets(true)
        }
      }
    }
  }
}
