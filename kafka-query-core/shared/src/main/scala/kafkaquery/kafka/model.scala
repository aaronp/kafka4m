package kafkaquery.kafka

sealed trait KafkaRequest
sealed trait KafkaResponse

final case class PartitionData(partition: Int, leader: String)

final case class ListTopicsResponse(topics: Map[String, Seq[PartitionData]]) extends KafkaResponse

final case class PullLatestResponse(topic: String, keys: Seq[String]) extends KafkaResponse

final case class StreamRequest(clientId: String, groupId: String, topic: String) extends KafkaRequest

sealed trait KafkaSupportRequest
final case class PublishMessage(topic: String, key: String, data: String) extends KafkaSupportRequest
