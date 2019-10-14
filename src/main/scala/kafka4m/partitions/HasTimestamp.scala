package kafka4m.partitions

import java.time.ZonedDateTime

import org.apache.kafka.clients.consumer.ConsumerRecord

/**
  * Represents a value which has a timestamp.
  *
  * This can then be used by [[Partitioner]]s to partition values of 'A' on a timestamp
  * @tparam A
  */
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
