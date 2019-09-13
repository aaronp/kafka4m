package kafka4m.partitions

import java.time.ZonedDateTime

import org.apache.kafka.clients.consumer.ConsumerRecord

trait HasTimestamp[A] {
  def timestamp(value: A): ZonedDateTime
}
object HasTimestamp {
  def apply[A](implicit ht: HasTimestamp[A]): HasTimestamp[A] = ht

  implicit def consumerRecordHasTimestamp[K, V] = new HasTimestamp[ConsumerRecord[K, V]] {
    override def timestamp(value: ConsumerRecord[K, V]): ZonedDateTime = {
      utcForEpochMillis(value.timestamp)
    }
  }
  implicit object identity extends HasTimestamp[ZonedDateTime] {
    override def timestamp(value: ZonedDateTime): ZonedDateTime = value
  }
}
