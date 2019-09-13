package kafka4m

import java.time.{ZoneId, ZonedDateTime}

package object partitions {

  val UTC                                                = ZoneId.of("UTC")
  def utcForEpochMillis(epochMilli: Long): ZonedDateTime = java.time.Instant.ofEpochMilli(epochMilli).atZone(UTC)
}
