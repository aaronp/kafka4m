package kafka4m.admin

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

/**
  * A case class representation of the Kafka Stats
  *
  * @param groupId the consumer group Id
  * @param offsetsByTopicPartition
  */
final case class ConsumerGroupStats(groupId: String, offsetsByTopicPartition: Map[TopicPartition, OffsetAndMetadata]) {

  /**
    * Filter the stats for just this topic
    * @param topic the topic we're interested in
    * @return a filtered ConsumerGroupStats
    */
  def forTopic(topic: String): ConsumerGroupStats = {
    def matchesTopic(topic: String)(tp: TopicPartition) = tp.topic() == topic
    copy(offsetsByTopicPartition = offsetsByTopicPartition.filterKeys(matchesTopic(topic)).toMap)
  }

  def topics: Set[String] = offsetsByTopicPartition.keys.map(_.topic()).toSet

  def offsetsByPartition: Map[Int, Long] = offsetsByTopicPartition.map {
    case (k1, value) => k1.partition() -> value.offset()
  }
  override def toString: String = {
    val entries = offsetsByTopicPartition.map {
      case (tp, offset) => s"\t$tp: ${offset.offset()} ${offset.metadata()}"
    }
    entries.mkString(s"Group '$groupId'\n", "\n", "\n")
  }
}
