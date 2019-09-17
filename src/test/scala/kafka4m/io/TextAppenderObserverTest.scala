package kafka4m.io

import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import cats.Show
import kafka4m.BaseKafka4mDockerSpec
import kafka4m.partitions.TimeBucketTest._
import kafka4m.partitions.{BatchEvent, FlushBucket, MiniBatchState, TimeBucket}
import kafka4m.util.Schedulers
import monix.reactive.Observable
import eie.io._

import scala.concurrent.duration._

class TextAppenderObserverTest extends BaseKafka4mDockerSpec {
  "TextAppenderObserver.fromEvents" should {
    "write the data to batches under a directory" in {
      Schedulers.using { implicit s =>
        withTmpDir { dir =>
          val disorderedTimes = for {
            hour <- (1 to 2)
            min  <- (0 to 59 by (3))
            time <- List(timeForHourAndMinute(hour, min + 2), timeForHourAndMinute(hour, min), timeForHourAndMinute(hour, min + 1))
          } yield {
            time
          }
          val input: Observable[ZonedDateTime] = Observable.fromIterable(disorderedTimes)
          implicit object showTime extends Show[ZonedDateTime] {
            override def show(t: ZonedDateTime): String = {
              DateTimeFormatter.ISO_DATE_TIME.format(t)
            }
          }

          val grouped: Observable[BatchEvent[ZonedDateTime, TimeBucket]] = {
            // let's group into 30 minute buckets
            val bucketMinutes = 30
            val buckets       = disorderedTimes.map(TimeBucket(bucketMinutes, _)).distinct
            val events        = MiniBatchState.byTime(input, 100, bucketMinutes.minutes)

            // in our test we want to flush all the buckets at the end. In normal runtime we listen for 'recordsReceivedBeforeFlush' messages
            // as a way to ensure we don't flush a bucket, and then immediately see a timestamped event which would've fallen within that
            // bucket
            events ++ Observable.fromIterable(buckets.map(FlushBucket.apply[ZonedDateTime, TimeBucket]))
          }
          val files: Observable[(TimeBucket, Path)] = TextAppenderObserver.fromEvents(dir, 1, 1, grouped)
          val byBucket: List[(TimeBucket, List[String])] = files
            .map {
              case (bucket, data: Path) => bucket -> data.text.linesIterator.toList
            }
            .toListL
            .runToFuture
            .futureValue

          byBucket.size shouldBe 4
          byBucket.toMap.keySet should contain only (
            TimeBucket(1, 0, 30),
            TimeBucket(1, 30, 60),
            TimeBucket(2, 0, 30),
            TimeBucket(2, 30, 60)
          )

          val FirstMinutes = s"2019-02-03T01:(\\d\\d):00Z\\[UTC\\]".r
          byBucket.toMap.apply(TimeBucket(1, 0, 30)).foreach { line =>
            val FirstMinutes(minute) = line
            minute.toInt should be >= 0
            minute.toInt should be <= 30
          }
          byBucket.toMap.apply(TimeBucket(1, 30, 60)).foreach { line =>
            val FirstMinutes(minute) = line
            minute.toInt should be >= 30
            minute.toInt should be <= 60
          }

          val SecondMinutes = s"2019-02-03T02:(\\d\\d):00Z\\[UTC\\]".r
          byBucket.toMap.apply(TimeBucket(2, 0, 30)).foreach { line =>
            val SecondMinutes(minute) = line
            minute.toInt should be >= 0
            minute.toInt should be <= 30
          }
          byBucket.toMap.apply(TimeBucket(2, 30, 60)).foreach { line =>
            val SecondMinutes(minute) = line
            minute.toInt should be >= 30
            minute.toInt should be <= 60
          }

        }
      }
    }
  }
}
