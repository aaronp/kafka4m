package kafka4m.util

import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption._
import java.nio.file.{Files, Path, Paths}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.Kafka4mApp
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.record.TimestampType

import scala.concurrent.duration._

/**
  * A place where we can write config stats for app support.
  *
  * It can periodically write the stats to disk so they can be served using some basic static html server
  */
class Stats private (config: Config) extends StrictLogging {
  import Stats._

  private val started = ZonedDateTime.now(ZoneId.of("UTC"))
  val enabled         = config.getBoolean("kafka4m.etl.stats.enabled")
  import args4c.implicits._
  private val htmlFlushFrequency         = config.asDuration("kafka4m.etl.stats.flushHtmlFrequency")
  private val writeToKafkaMetrics        = new Metrics()
  private val readFromKafkaMetrics       = new Metrics()
  private var nextHtmlFlushDue           = 0L
  private var readFromKafkaDetailedStats = KafkaReadStats(Map.empty)
//  private var writeToKafkaStats  = LatencySnapshot(-1, 0)
//  private var writeToKafkaMetrics  = LatencySnapshot(-1, 0)

  private val writeToOpt: Option[Path] = {
    config.getString("kafka4m.etl.stats.writeTo") match {
      case "" => None
      case path =>
        val dir = Paths.get(path)
        if (!Files.isDirectory(dir)) {
          logger.info(s"Writing stats to $path (creating...)")
          Files.createDirectories(dir)
        } else {
          logger.info(s"Writing stats to $path")
        }
        val confSummary: String = Kafka4mApp.summary(config)
        setContent(dir.resolve("config.txt"), confSummary)
        Option(dir)
    }
  }

  private[util] def indexHtml(writeToKafkaSnapshot: LatencySnapshot, readFromKafkaSnapshot: LatencySnapshot): String = {
    val startTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(started).map {
      case x if x.isLetter => ' '
      case x               => x
    }

    val writeHtml = if (writeToKafkaSnapshot.nonEmpty) {
      s"<div><h3>Written to Kafka</h3>${writeToKafkaSnapshot.toString}</div>"
    } else {
      ""
    }
    val readHtml = if (readFromKafkaSnapshot.nonEmpty) {
      s"<div><h3>Read from Kafka</h3>$readFromKafkaSnapshot</div><div>${readFromKafkaDetailedStats.renderHtml}</div>"
    } else {
      ""
    }
    s"""<html>
      |<h1>Kafka4m</h1>
      |<div>Started ${startTime}</div>
      |  <ul>
      |    <li><a href="config.txt">config</a></li>
      |  </ul>
      |$writeHtml
      |$readHtml
      |</html>""".stripMargin
  }

  def flushHtml(report: => String) = {
    writeToOpt.fold(false) { dir =>
      setContent(dir.resolve("index.html"), report)
      true
    }
  }

  val onWriteToKafka = writeToKafkaMetrics.incThroughput
  def onReadFromKafka(record: ConsumerRecord[_, _]): Task[Unit] = {
    val now = Task.now[Unit] {
      readFromKafkaDetailedStats = readFromKafkaDetailedStats.update(record)
    }
    now.flatMap(_ => readFromKafkaMetrics.incThroughput)
  }

  def start(scheduler: Scheduler): Cancelable = {
    if (enabled) {
      scheduler.scheduleAtFixedRate(1.second, 1.second) {
        flush()
      }
    } else {
      Cancelable.empty
    }
  }

  def flush(): Unit = {
    val writeToKafkaStats: LatencySnapshot = writeToKafkaMetrics.flush()
    if (writeToKafkaStats.nonEmpty) {
      logger.info(s"Uploaded ${writeToKafkaStats}")
    }
    val readFromKafkaStats = readFromKafkaMetrics.flush()
    if (readFromKafkaStats.nonEmpty) {
      logger.info(s"Read ${readFromKafkaStats}")
    }

    val now = System.currentTimeMillis
    if (htmlFlushFrequency.toMillis > 0 && now >= nextHtmlFlushDue) {
      flushHtml(indexHtml(writeToKafkaStats, readFromKafkaStats))
      nextHtmlFlushDue = now + htmlFlushFrequency.toMillis
    }
  }
  private def setContent(path: Path, text: String): Path = {
    Files.write(path, text.getBytes(StandardCharsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)
  }
}

object Stats {

  def apply(config: Config): Stats = new Stats(config)

  case class LatencyPoint(received: Long, record: ConsumerRecord[_, _]) {
    def describe: String = {
      val tsStr = record.timestampType match {
        case TimestampType.NO_TIMESTAMP_TYPE => ""
        case _                               => s"latency ${latencyInMs}ms ts (${record.timestampType} ${record.timestamp}, "
      }
      s"${tsStr}(offset ${record.offset}, partition ${record.partition}, key ${record.key})"
    }
    private def latencyInMs: Long = {
      record.timestampType match {
        case TimestampType.NO_TIMESTAMP_TYPE => 0L
        case _                               => received - record.timestamp
      }
    }
    def sortKey: Long = {
      record.timestampType match {
        case TimestampType.NO_TIMESTAMP_TYPE => received
        case _                               => received - record.timestamp
      }
    }
    def min(other: LatencyPoint): LatencyPoint = {
      if (sortKey < other.sortKey) this else other
    }
    def max(other: LatencyPoint): LatencyPoint = {
      if (sortKey > other.sortKey) this else other
    }

  }
  case class LatencyStats(min: LatencyPoint, max: LatencyPoint, latest: LatencyPoint) {
    def update(point: LatencyPoint): LatencyStats = {
      LatencyStats(point.min(min), point.max(max), point)
    }

    def renderHtml: String = {
      Seq( //
          min.describe, //
          max.describe, //
          latest.describe).mkString( //
                                    "<td>",
                                    "</td><td>",
                                    "</td>" //
      )
    }
  }
  object LatencyStats {
    def apply(first: LatencyPoint): LatencyStats = {
      new LatencyStats(first, first, first)
    }
  }
  case class PartitionStats(minOffset: Long, maxOffset: Long, latency: LatencyStats)
  case class KafkaReadStats(partitions: Map[Int, PartitionStats]) {
    def nonEmpty = partitions.nonEmpty
    def renderHtml: String = {
      val head = Seq("partition", "total", "min offset", "max offset", "min latency", "max latency", "latest latency").mkString( //
                                                                                                                                "<thead><tr><td>", //
                                                                                                                                "</td><td>", //
                                                                                                                                "</td></tr></thead>" //
      )
      partitions.keySet.toList.sorted
        .map { p =>
          val PartitionStats(min, max, latency) = partitions(p)
          s"""<tr><td>$p</td><td>${max - min}</td><td>$min</td><td>$max</td>${latency.renderHtml}</tr>""".stripMargin
        }
        .mkString(s"<table margin=4 padding=4 >${head}<tbody>", "\n", "\n</tbody></table>")
    }

    def update(record: ConsumerRecord[_, _]): KafkaReadStats = {
      val latency = LatencyPoint(System.currentTimeMillis, record)
      val newStats = partitions.get(record.partition) match {
        case Some(PartitionStats(min, max, stats)) =>
          PartitionStats(record.offset.min(min), record.offset.max(max), stats.update(latency))
        case None => PartitionStats(record.offset, record.offset, LatencyStats(latency))
      }
      copy(partitions.updated(record.partition, newStats))
    }
  }
}
