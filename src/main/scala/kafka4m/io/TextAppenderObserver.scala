package kafka4m.io

import java.io.{BufferedWriter, FileOutputStream, OutputStreamWriter}
import java.nio.file.{Files, Path}
import java.util.Base64

import cats.Show
import com.typesafe.scalalogging.StrictLogging
import kafka4m.partitions._
import kafka4m.{Bytes, Key}
import monix.execution.Ack
import monix.reactive.{Observable, Observer}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

/**
  * A namespace which holds a 'fromEvents' as a means to transform append events into time-bucketed files
  */
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

  /**
    * A means to represent a byte-array record as a base-64 encoded text value
    */
  implicit object ShowRecord extends Show[ConsumerRecord[Key, Bytes]] {
    override def show(record: ConsumerRecord[Key, Bytes]): String = {
      val dataString = Base64.getEncoder.encodeToString(record.value)
      s"${record.partition}_${record.offset}_${record.timestamp}_${record.timestampType}:${dataString}"
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
  def fromEvents[A: HasTimestamp: Show](dir: Path,
                                        flushEvery: Int,
                                        bucketRangeInMinutes: Int,
                                        appendEvents: Observable[PartitionEvent[A, TimeBucket]]): Observable[(TimeBucket, Path)] = {
    implicit val partitioner: Partitioner[A, TimeBucket] = Partitioner.byTime[A](bucketRangeInMinutes)

    val partitionedByTime = PartitionAppenderState.partitionEvents[A, TimeBucket, TextAppenderObserver[A]](appendEvents) {
      case (bucket, firstValue) =>
        val file = dir.resolve(bucket.asFileName(HasTimestamp[A].timestamp(firstValue)))
        new TextAppenderObserver(file, flushEvery)
    }

    partitionedByTime.map {
      case (bucket, appender) => (bucket, appender.file)
    }
  }
}
class TextAppenderObserver[A: Show](val file: Path, flushEvery: Int = 10) extends Observer[A] with Appender[A] with StrictLogging {
  require(flushEvery >= 0)
  if (!Files.exists(file)) {
    Files.createFile(file)
  }
  private val writer       = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.toFile, false)))
  private var flushCount   = flushEvery
  private var written      = 0
  private var closed       = false
  private val dataToString = Show[A]

  override def append(data: A): Unit = {
    if (written > 0) {
      writer.newLine()
    }
    writer.write(dataToString.show(data))
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

  override def onNext(elem: A): Future[Ack] = {
    append(elem)
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
