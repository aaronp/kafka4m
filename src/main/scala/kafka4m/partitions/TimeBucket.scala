package kafka4m.partitions

import java.time.ZonedDateTime

/** Represents a key based on an hour and minute range
  * @param hour
  * @param fromMinute
  * @param toMinute
  */
final case class TimeBucket(hour: Int, fromMinute: Int, toMinute: Int) {

  /** @param time the time to format
    * @return a filename for the given time
    */
  def asFileName(time: ZonedDateTime) = {
    s"${time.getYear}-${time.getMonthValue}-${time.getDayOfMonth}__${hour}hr_${fromMinute}-${toMinute}.txt"
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
