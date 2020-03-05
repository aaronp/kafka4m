package kafka4m.consumer

import kafka4m.data.PartitionOffsetState
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

/**
  * Represents a message with the components needed to commit the offsets/partitions to Kafka
  * @param access
  * @param offset
  * @param record
  * @tparam A
  */
final case class AckableRecord[A] private[consumer] (access: ConsumerAccess, offset: PartitionOffsetState, record: A) extends ConsumerAccess {

  override type Key   = access.Key
  override type Value = access.Value

  /**
    * Commits the current partition offset + 1 to Kafka so that, should we disconnect, we'll receive the next message
    * @return a future of the offsets
    */
  def commitPosition(): Future[Map[TopicPartition, OffsetAndMetadata]] = commit(offset.incOffsets())

  def commit(offset: PartitionOffsetState) = commitAsync(offset).flatten

  /**
    * If we commit this offset, then on reconnect we would receive this same message again
    *
    * @return the commit future
    */
  def commitConsumedPosition(): Future[Map[TopicPartition, OffsetAndMetadata]] = {
    commit(offset)
  }

  /**
    * Maps the record type for this record
    * @param f
    * @tparam B
    * @return
    */
  final def map[B](f: A => B): AckableRecord[B] = copy(access, offset, f(record))

  def commitAsync(state: PartitionOffsetState) = withConsumer(_.commitAsync(state))

  override def withConsumer[A](thunk: RichKafkaConsumer[Key, Value] => A): Future[A] = {
    access.withConsumer(thunk)
  }
}

object AckableRecord {

  /**
    * combine the records with a means of tracking the offsets
    * @param records
    * @return
    */
  def withOffsets[A: HasRecord](records: Observable[A]): Observable[(PartitionOffsetState, A)] = {
    val initialTuple: (PartitionOffsetState, Option[A]) = (PartitionOffsetState(), Option.empty[A])
    records
      .scan(initialTuple) {
        case ((state, _), value) =>
          val kafkaMsg = HasRecord[A].recordFor(value)
          (state.update(kafkaMsg), Option(value))
      }
      .collect {
        case (state, Some(a)) => (state, a)
      }
  }

}
