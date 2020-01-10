package kafka4m.consumer

import java.util

import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

/**
  * an immutable means of tracking which offset/partitions have been observed
  * @param offsetByPartitionByTopic
  */
final case class PartitionOffsetState(offsetByPartitionByTopic: Map[String, Map[Int, Long]] = Map.empty) {

  /**
    * We typically keep a [[PartitionOffsetState]] updated with observed kafka [[ConsumerRecord]]s.
    * A normal workflow periodically (or even every message) tells Kafka vai [[RichKafkaConsumer]] to commit based on [[RichKafkaConsumer.commitAsync]].
    *
    * If we passed in this last [[PartitionOffsetState]] observed and quit, then Kafka would return the same record we last observed (e.g. potentially an off-by-one error).
    * If we have a system which is intolerable to processing duplicate messages, then we should really tell Kafka to commit the _next_ offset of the last message observed/processed
    */
  def incOffsets() = {
    val next = offsetByPartitionByTopic.mapValues { offsetMap =>
      val nextOffset = offsetMap.mapValues(_ + 1)
      nextOffset.toMap
    }
    copy(offsetByPartitionByTopic = next.toMap)
  }

  def nonEmpty: Boolean = offsetByPartitionByTopic.nonEmpty

  def asTopicPartitionMapJava: util.Map[TopicPartition, OffsetAndMetadata] = asTopicPartitionMap.asJava

  def asTopicPartitionMap: Map[TopicPartition, OffsetAndMetadata] = {
    offsetByPartitionByTopic.foldLeft(Map.empty[TopicPartition, OffsetAndMetadata]) {
      case (accum1, (topic, offsets)) =>
        offsets.foldLeft(accum1) {
          case (accum2, (part, offset)) =>
            val key               = new TopicPartition(topic, part)
            val offsetAndMetadata = new OffsetAndMetadata(offset)
            accum2.updated(key, offsetAndMetadata)
        }
    }
  }

  def update(record: ConsumerRecord[_, _]): PartitionOffsetState = update(record.topic(), record.partition(), record.offset())

  def update(topic: String, partition: Int, offset: Long): PartitionOffsetState = {
    val offsetByPartition = offsetByPartitionByTopic.getOrElse(topic, Map.empty)
    val newMap            = offsetByPartition.updated(partition, offset)
    PartitionOffsetState(offsetByPartitionByTopic.updated(topic, newMap))
  }

}
object PartitionOffsetState {
  def apply(entries: (String, Map[Int, Long])*)      = new PartitionOffsetState(entries.toMap.ensuring(_.size == entries.size))
  def forTopic(topic: String)(entries: (Int, Long)*) = new PartitionOffsetState(Map(topic -> entries.toMap.ensuring(_.size == entries.size)))
}
