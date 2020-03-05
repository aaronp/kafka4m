package kafka4m.data

import java.util

import kafka4m.consumer.RichKafkaConsumer
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition

import scala.jdk.CollectionConverters._

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
  def incOffsets(delta: Int = 1): PartitionOffsetState = {
    val next = offsetByPartitionByTopic.view.mapValues { offsetMap =>
      val nextOffset = offsetMap.view.mapValues(_ + delta)
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

  def fromKafka(offsetAndMetadataByTopicPartition: Map[TopicPartition, OffsetAndMetadata]) = {
    val initial = Map.empty[String, Map[Int, Long]]
    val scalaMap = offsetAndMetadataByTopicPartition.foldLeft(initial) {
      case (acc, (null, _)) => acc
      case (acc, (_, null)) => acc
      case (acc, (topicPartition, offsetAndMD)) =>
        require(topicPartition != null, s"WTF Kafka ... topicPartition is null!")
        require(offsetAndMD != null, s"WTF Kafka ... offsetAndMD is null!")
        val offsetByPartition = acc.getOrElse(topicPartition.topic(), Map[Int, Long]())
        val p                 = topicPartition.partition()
        val o                 = offsetAndMD.offset()
        val newMap            = offsetByPartition.updated(p, o)
        acc.updated(topicPartition.topic(), newMap)
    }
    new PartitionOffsetState(scalaMap)
  }

  case class LaterOffsetsResetToEarlierPosition(mismatchesByTopic: Map[String, Set[OffsetMismatch]]) extends Exception

  def checkOffsets(earlierState: PartitionOffsetState, laterState: PartitionOffsetState) = {
    LaterOffsetsResetToEarlierPosition.mismatchesByTopic(earlierState, laterState) match {
      case set if set.nonEmpty => Some(LaterOffsetsResetToEarlierPosition(set.toMap))
      case _                   => None
    }
  }

  object LaterOffsetsResetToEarlierPosition {

    private def mismatchesForOffsets(earlierOffsetByPartition: Map[Int, Long], laterOffsetByPartition: Map[Int, Long]) = {
      val partitions = earlierOffsetByPartition.keySet ++ laterOffsetByPartition.keySet
      partitions.collect { partition =>
        (earlierOffsetByPartition.get(partition), laterOffsetByPartition.get(partition)) match {
          // we ignore any missing partitions, as presumably leaving those out in a kermit request wouldn't affect anything
          case (Some(shouldBeEarlierOffset), Some(shouldBeLaterOffset)) if shouldBeLaterOffset < shouldBeEarlierOffset =>
            OffsetMismatch(partition, shouldBeEarlierOffset, shouldBeLaterOffset)
        }
      }
    }
    def mismatchesByTopic(earlierState: PartitionOffsetState, laterState: PartitionOffsetState) = {
      val topics = laterState.offsetByPartitionByTopic.keySet ++ earlierState.offsetByPartitionByTopic.keySet
      topics.flatMap { topic =>
        (earlierState.offsetByPartitionByTopic.get(topic), laterState.offsetByPartitionByTopic.get(topic)) match {
          case (Some(earlier), Some(later)) =>
            val mismatches = mismatchesForOffsets(earlier, later)
            if (mismatches.nonEmpty) {
              Option(topic -> mismatches)
            } else {
              None
            }
          case _ => None
        }
      }
    }
  }

  case class OffsetMismatch(partition: Int, earlierOffset: Long, laterOffset: Long)

}
