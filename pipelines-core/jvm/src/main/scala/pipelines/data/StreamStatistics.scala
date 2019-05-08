package pipelines.data

import java.time.ZonedDateTime

case class StreamStatistics[A](mostRecent: Option[A],
                               totalReceived: Long,
                               connectedAt: ZonedDateTime,
                               latestRatePerSecond: Int,
                               averageRatePerSecond: Int,
                               lastReceivedAt: ZonedDateTime)
