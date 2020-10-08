package kafka4m.data

import org.apache.kafka.common._
case class KafkaPartitionInfo(topic: String,
                              partition: Int,
                              leader: Option[KafkaNode],
                              replicas: List[KafkaNode],
                              inSyncReplicas: List[KafkaNode],
                              offlineReplicas: List[KafkaNode]) {

  def asTopicPartition = new TopicPartition(topic, partition)

  override def toString = {
    s"""KafkaPartitionInfo($topic, partition $partition)
      |            leader : $leader
      |            ${replicas.size} replicas : ${replicas.mkString("[", ",", "]")}
      |            ${inSyncReplicas.size} inSyncReplicas : ${inSyncReplicas.mkString("[", ",", "]")}
      |            ${offlineReplicas.size} offlineReplicas : ${offlineReplicas.mkString("[", ",", "]")}
      |""".stripMargin
  }
}

object KafkaPartitionInfo {
  def apply(info: PartitionInfo): KafkaPartitionInfo = {
    new KafkaPartitionInfo(
      topic = Option(info.topic()).getOrElse(""),
      partition = info.partition,
      leader = Option(info.leader()).map(KafkaNode.apply),
      replicas = info.replicas().map(KafkaNode.apply).toList,
      inSyncReplicas = info.inSyncReplicas().map(KafkaNode.apply).toList,
      offlineReplicas = info.offlineReplicas().map(KafkaNode.apply).toList
    )
  }
}
