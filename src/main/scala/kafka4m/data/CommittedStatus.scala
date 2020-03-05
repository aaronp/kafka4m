package kafka4m.data

case class CommittedStatus(topic: String, subscribed: Boolean, partitionStats: List[CommittedStatus.AssignedPartition], partitionOffsetState: PartitionOffsetState) {

  lazy val offsets: Map[Int, Long] = partitionOffsetState.offsetByPartitionByTopic.getOrElse(topic, Map.empty[Int, Long])

  override def toString: String = {

    s"""CommittedStatus($topic)
       |  Subscribed: $subscribed
       |  PartitionStats: ${partitionStats.mkString("\n\t\t", "\n\t\t", "")}
       |  Offsets:${offsets.mkString("\n\t\t", "\n\t\t", "")}
       |""".stripMargin
  }

}

object CommittedStatus {

  type IsAssigned        = Boolean
  type AssignedPartition = (KafkaPartitionInfo, IsAssigned)
}
