package kafka4m.io

import java.nio.file.{Files, Path, Paths}

import cats.Show
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.partitions.{AppendEvent, HasTimestamp, TimeBucket, TimePartitionState}
import kafka4m.{Bytes, Key}
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration._

/**
  * An ETL configuration which will take Kafka records and write them to some directory
  *
  * @param dir the directory to write the buckets under
  * @param recordsReceivedBeforeClosingBucket see comments in reference.conf
  * @param numberOfAppendsBeforeWriterFlush see comments in reference.conf
  * @param timeBucketMinutes see comments in reference.conf
  * @tparam A the record type
  */
case class Base64Writer[A: HasTimestamp: Show](dir: Path, recordsReceivedBeforeClosingBucket: Int, numberOfAppendsBeforeWriterFlush: Int, timeBucketMinutes: Int) {

  def asEvents(input: Observable[A]): Observable[AppendEvent[A]] = {
    TimePartitionState.appendEvents(input, recordsReceivedBeforeClosingBucket, timeBucketMinutes.minutes)
  }

  def partition(input: Observable[A]): Observable[(TimeBucket, Path)] = {
    write(asEvents(input))
  }

  def write(events: Observable[AppendEvent[A]]): Observable[(TimeBucket, Path)] = {
    TextAppenderObserver.fromEvents(dir, numberOfAppendsBeforeWriterFlush, events)
  }
}

object Base64Writer extends StrictLogging {

  def apply(rootConfig: Config): Base64Writer[ConsumerRecord[Key, Bytes]] = {
    val fromKafkaConfig = rootConfig.getConfig("kafka4m.etl.fromKafka")
    forConfig(fromKafkaConfig)
  }

  def forConfig(fromKafkaConfig: Config): Base64Writer[ConsumerRecord[Key, Bytes]] = {
    val dataDir = {
      val dirName   = fromKafkaConfig.getString("dataDir")
      val createDir = fromKafkaConfig.getBoolean("createDirIfMissing")
      val dir       = Paths.get(dirName)
      if (!Files.isDirectory(dir) && createDir) {
        val ok = Files.createDirectories(dir)
        logger.info(s"Creating write directory ${ok}")
      }
      dir
    }

    implicit val show = TextAppenderObserver.ShowRecord
    new Base64Writer[ConsumerRecord[Key, Bytes]](
      dir = dataDir,
      recordsReceivedBeforeClosingBucket = fromKafkaConfig.getInt("recordsReceivedBeforeClosingBucket"),
      numberOfAppendsBeforeWriterFlush = fromKafkaConfig.getInt("numberOfAppendsBeforeWriterFlush"),
      timeBucketMinutes = fromKafkaConfig.getInt("timeBucketMinutes")
    )
  }
}
