package kafka4m.io

import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.typesafe.scalalogging.StrictLogging
import kafka4m._
import kafka4m.io.ZipAppenderObserver.ToEntry
import kafka4m.partitions.{HasTimestamp, PartitionEvent, Partitioner, TimeBucket}
import monix.execution.Ack
import monix.reactive.{Observable, Observer}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

object ZipAppenderObserver {

  trait ToEntry[A] {
    def toEntry(value: A): (Key, Bytes)
  }

  object ToEntry {
    def apply[A](implicit toBytes: ToEntry[A]): ToEntry[A] = toBytes

    implicit object identity extends ToEntry[(Key, Bytes)] {
      override def toEntry(value: (Key, Bytes)): (Key, Bytes) = value
    }
    implicit object FromConsumerRecord extends ToEntry[ConsumerRecord[Key, Bytes]] {
      override def toEntry(value: ConsumerRecord[Key, Bytes]): (Key, Bytes) = {
        value.key -> value.value
      }
    }
  }

  /**
    *
    * @param dir
    * @param flushEvery
    * @param bucketRangeInMinutes the number
    * @param appendEvents the input events
    * @tparam A
    * @return an observable of the time buckets and the file which was written which contains the base64-encoded data in that bucket
    */
  def fromEvents[A: HasTimestamp: ToEntry](dir: Path,
                                           flushEvery: Int,
                                           bucketRangeInMinutes: Int,
                                           appendEvents: Observable[PartitionEvent[A, TimeBucket]],
                                           zipLevel: Int = -1): Observable[(TimeBucket, Path)] = {
    implicit val partitioner: Partitioner[A, TimeBucket] = Partitioner.byTime[A](bucketRangeInMinutes)

    val partitionedByTime = PartitionAppenderState.partitionEvents[A, TimeBucket, ZipAppenderObserver[A]](appendEvents) {
      case (bucket, firstValue) =>
        val file = dir.resolve(bucket.asFileName(HasTimestamp[A].timestamp(firstValue)))
        new ZipAppenderObserver(file, flushEvery, zipLevel)
    }

    partitionedByTime.map {
      case (bucket, appender) => (bucket, appender.zipFile)
    }
  }
}
class ZipAppenderObserver[A: ToEntry](val zipFile: Path, flushEvery: Int = 10, zipLevel: Int = -1) extends Observer[A] with Appender[A] with AutoCloseable with StrictLogging {

  require(flushEvery >= 0)
  if (!Files.exists(zipFile)) {
    Files.createFile(zipFile)
  }

  val zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile), StandardCharsets.UTF_8)
  zipOut.setLevel(zipLevel)
  var flushCount = flushEvery

  override def onNext(elem: A): Future[Ack] = {
    append(elem)
    Ack.Continue
  }

  override def onError(ex: Throwable): Unit = {
    logger.error(s"error: $ex ", ex)
    close()
  }

  override def onComplete(): Unit = {
    logger.info("onComplete")
    close()
  }

  override def close(): Unit = {
    logger.info("closing...")
    zipOut.flush()
    zipOut.close()
  }

  override def append(value: A): Unit = {
    val (name, data) = ToEntry[A].toEntry(value)
    val entry        = new ZipEntry(name)
    zipOut.putNextEntry(entry)
    zipOut.write(data)
    zipOut.closeEntry()

    flushCount = flushCount - 1
    if (flushCount <= 0) {
      flushCount = flushEvery
      zipOut.flush()
    }
  }
}
