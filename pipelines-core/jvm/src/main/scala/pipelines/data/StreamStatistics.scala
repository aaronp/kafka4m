package pipelines.data

import java.time.ZonedDateTime

import io.circe.{Decoder, Encoder}

// : Encoder :Decoder
case class StreamStatistics[A](mostRecent: Option[A],
                               totalReceived: Long,
                               connectedAt: ZonedDateTime,
                               latestRatePerSecond: Int,
                               averageRatePerSecond: Int,
                               lastReceivedAt: ZonedDateTime)

object StreamStatistics {}
