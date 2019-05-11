package pipelines.reactive

import java.time.Duration.between
import java.time.ZonedDateTime

import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

case class StreamStatistics(totalReceived: Long,
                            connectedAt: ZonedDateTime,
                            latestRatePerSecond: Int,
                            maxRatePerSecond: Int,
                            averageRatePerSecond: Int,
                            lastReceivedAt: ZonedDateTime)

object StreamStatistics {
  def fromObservable[A](sampleFreq: FiniteDuration, source: Observable[A]): Observable[(Option[A], StreamStatistics)] = {
    val first = StreamStatistics(0, ZonedDateTime.now, 0, 0, 0, ZonedDateTime.now)
    val adjustPerSecond = (1000.toDouble / sampleFreq.toMillis)

    source.bufferTimed(sampleFreq).scan(Option.empty[A] -> first) {
      case ((_, stats), seq) =>
        val now   = ZonedDateTime.now
        val total = stats.totalReceived + seq.size
        val avePerSecond = between(stats.connectedAt, now).getSeconds match {
          case 0       => 0
          case seconds => (total / seconds).toInt
        }
        val latestPerSecond  = (seq.size * adjustPerSecond).toInt
        val maxRatePerSecond = stats.maxRatePerSecond.max(latestPerSecond)
        seq.lastOption -> StreamStatistics(total, stats.connectedAt, latestPerSecond, maxRatePerSecond, avePerSecond, now)
    }
  }
}
