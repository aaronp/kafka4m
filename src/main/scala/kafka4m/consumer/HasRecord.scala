package kafka4m.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord

/**
  * Represents something from which we can obtain a ConsumerRecord
  * @tparam A the input value type (typically a tuple, list, etc)
  */
trait HasRecord[A] {

  /**
    * @param value the input value
    * @return the Kafka record for this type
    */
  def recordFor(value: A): ConsumerRecord[_, _]

}

object HasRecord {
  def apply[A](implicit value: HasRecord[A]) = value

  implicit def fromTuple2[K, V, T] = new HasRecord[(T, ConsumerRecord[K, V])] {
    override def recordFor(value: (T, ConsumerRecord[K, V])) = value._2
  }
}
