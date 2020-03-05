package kafka4m.consumer

import kafka4m.data.KafkaPartitionInfo

final case class TopicStatus(topic: String, partitions: Seq[KafkaPartitionInfo]) {

  override def toString = {
    partitions.mkString(s"Topic: ${topic}:\n\t", "\n\t", "\n\t")
  }
}
