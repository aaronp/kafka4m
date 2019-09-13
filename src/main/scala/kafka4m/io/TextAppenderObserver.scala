package kafka4m.io

import java.io.{BufferedWriter, FileOutputStream, OutputStreamWriter}
import java.nio.file.{Files, Path}
import java.time.ZonedDateTime
import java.util.Base64

import cats.Show
import com.typesafe.scalalogging.StrictLogging
import kafka4m.partitions._
import kafka4m.{Bytes, Key}
import monix.execution.Ack
import monix.reactive.{Notification, Observable, Observer}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

object TextAppenderObserver {

  private val KeyValueR = """(.*?):(.*)""".r

  /**
    * The 'show' for byte arrays is written to files as:
    * {{{
    *   <key>:<base 64>
    * }}}
    *
    *  This extractor will parse a line as taken from an appended file
    */
  object Base64Line {
    def unapply(line: String): Option[(String, Array[Byte])] = {
      line match {
        case KeyValueR(key, value) => Option(key -> Base64.getDecoder.decode(value))
        case _                     => None
      }
    }
  }

  implicit object ShowRecord extends Show[ConsumerRecord[Key, Bytes]] {
    override def show(record: ConsumerRecord[Key, Bytes]): String = {
      val dataString = Base64.getEncoder.encodeToString(record.value)
      s"${record.partition}_${record.offset}_${record.timestamp}_${record.timestampType}:${dataString}"
    }
  }

  def asFileName(first: ZonedDateTime, bucket: TimeBucket) = {
    s"${first.getYear}-${first.getMonthValue}-${first.getDayOfMonth}__${bucket.hour}hr_${bucket.fromMinute}-${bucket.toMinute}.txt"
  }

  def fromEvents[A: HasTimestamp: Show](dir: Path, flushEvery: Int, appendEvents: Observable[AppendEvent[A]]): Observable[(TimeBucket, Path)] = {
    appendEvents
      .scan(GroupState[A](dir, flushEvery, Map.empty) -> Seq.empty[Notification[(TimeBucket, Path)]]) {
        case ((st8, _), next) => st8.update(next)
      }
      .flatMap {
        case (_, notifications) => Observable.fromIterable(notifications)
      }
      .dematerialize
  }

  private case class GroupState[A: HasTimestamp: Show](dir: Path, flushEvery: Int, byBucket: Map[TimeBucket, TextAppenderObserver]) {
    private val NoOp = (this, Nil)
    def update(event: AppendEvent[A]): (GroupState[A], Seq[Notification[(TimeBucket, Path)]]) = {
      event match {
        case AppendData(bucket, data) =>
          val text = Show[A].show(data)
          byBucket.get(bucket) match {
            case Some(appender) =>
              appender.appendLine(text)
              NoOp
            case None =>
              val file     = dir.resolve(asFileName(HasTimestamp[A].timestamp(data), bucket))
              val appender = new TextAppenderObserver(file, flushEvery)
              appender.appendLine(text)
              copy(byBucket = byBucket.updated(bucket, appender)) -> Nil
          }

        case ForceFlushBuckets() =>
          val onNexts = byBucket.map {
            case (bucket, appender) =>
              appender.close()
              Notification.OnNext(bucket -> appender.file)
          }
          copy(byBucket = Map.empty) -> (onNexts.toSeq :+ Notification.OnComplete)

        case FlushBucket(bucket) =>
          byBucket.get(bucket) match {
            case Some(appender) =>
              appender.close()
              copy(byBucket = byBucket - bucket) -> Seq(Notification.OnNext(bucket -> appender.file))
            case None =>
              NoOp
          }
      }
    }
  }
}
class TextAppenderObserver(val file: Path, flushEvery: Int = 10) extends Observer[String] with AutoCloseable with StrictLogging {
  require(flushEvery >= 0)
  if (!Files.exists(file)) {
    Files.createFile(file)
  }
  private val writer     = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.toFile, false)))
  private var flushCount = flushEvery
  private var written    = 0
  private var closed     = false

  def appendLine(line: String): Unit = {
    if (written > 0) {
      writer.newLine()
    }
    writer.write(line)
    written = written + 1
    flushCount = flushCount - 1
    if (flushCount <= 0) {
      flush()
    }
  }

  def flush(): Unit = {
    flushCount = flushEvery
    writer.flush()
  }

  override def onNext(elem: String): Future[Ack] = {
    appendLine(elem)
    Ack.Continue
  }

  override def onError(ex: Throwable): Unit = {
    logger.error(s"error: $ex", ex)
    close()
  }

  override def onComplete(): Unit = {
    logger.info("onComplete")
    close()
  }

  def isClosed(): Boolean = closed

  def size(): Int = written

  override def close(): Unit = {
    logger.info(s"closing ${file}")
    writer.flush()
    writer.close()
    closed = true
  }
}
