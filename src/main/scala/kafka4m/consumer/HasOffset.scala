package kafka4m.consumer

import kafka4m.data.PartitionOffsetState

/**
  * the functions required by [[ConcurrentStream]] to commit offsets back to kafka
  * @tparam A
  */
trait HasOffset[A] { self =>
  def offsetForValue(value: A): PartitionOffsetState

}
object HasOffset {

  case class Const[A](data: A, fixedOffset: PartitionOffsetState)

  implicit def constHasOffset[A]: HasOffset[Const[A]] = new HasOffset[Const[A]] {
    override def offsetForValue(value: Const[A]): PartitionOffsetState = value.fixedOffset
  }

  implicit def forAckRecord[K, V, A]: HasOffset[AckableRecord[A]] = new HasOffset[AckableRecord[A]] {
    override def offsetForValue(value: AckableRecord[A]): PartitionOffsetState = value.offset
  }
}
