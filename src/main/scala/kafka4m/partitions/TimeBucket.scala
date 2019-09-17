package kafka4m.partitions

import java.time.ZonedDateTime

/**
  *
  * @param hour
  * @param fromMinute
  * @param toMinute
  */
final case class TimeBucket(hour: Int, fromMinute: Int, toMinute: Int) {
  def asFileName(first: ZonedDateTime) = {
    s"${first.getYear}-${first.getMonthValue}-${first.getDayOfMonth}__${hour}hr_${fromMinute}-${toMinute}.txt"
  }
}
object TimeBucket {
  def apply(minutes: Int, epochMilli: Long): TimeBucket = {
    apply(minutes, utcForEpochMillis(epochMilli))
  }

  def apply(minutes: Int, zonedDT: ZonedDateTime): TimeBucket = {
    val bucketIndex = zonedDT.getMinute / minutes
    val from        = bucketIndex * minutes
    new TimeBucket(zonedDT.getHour, from, toMinute = from + minutes)
  }
}
