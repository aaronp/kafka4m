package kafka4m.consumer

import org.apache.kafka.common.PartitionInfo

final case class TopicStatus(topic: String, partitions: Seq[PartitionInfo]) {

  override def toString = {
    partitions.mkString(s"Topic: ${topic}:\n\t", "\n\t", "\n\t")
  }
}
