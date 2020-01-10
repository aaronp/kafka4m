package kafka4m.admin

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition

final case class ConsumerGroupStats(groupId: String, offsetsByTopicPartition: Map[TopicPartition, OffsetAndMetadata]) {

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
