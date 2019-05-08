package pipelines.kafka

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import pipelines.core.{Rate, StreamStrategy}

final case class ResponseFormat(fields: Seq[String])

object ResponseFormat {
  implicit def decoder = deriveDecoder[ResponseFormat]
  implicit def encoder = deriveEncoder[ResponseFormat]
}

final case class QueryRequest(clientId: String,
                              groupId: String,
                              topic: String,
                              filterExpression: String,
                              filterExpressionIncludeMatches: Boolean,
                              fromOffset: Option[String],
                              messageLimit: Option[Rate],
                              format: Option[ResponseFormat],
                              streamStrategy: StreamStrategy)
    extends KafkaRequest

object QueryRequest {
  implicit val decoder = deriveDecoder[QueryRequest]
  implicit val encoder = deriveEncoder[QueryRequest]
}

final case class UpdateFeedRequest(query: QueryRequest)
object UpdateFeedRequest {
  implicit def decoder                                   = deriveDecoder[UpdateFeedRequest]
  implicit def encoder: ObjectEncoder[UpdateFeedRequest] = deriveEncoder[UpdateFeedRequest]
}

sealed trait KafkaRequest
sealed trait KafkaResponse

final case class PartitionData(partition: Int, leader: String)
object PartitionData {
  implicit val encoder = deriveEncoder[PartitionData]
  implicit val decoder = deriveDecoder[PartitionData]
}

final case class ListTopicsResponse(topics: Map[String, Seq[PartitionData]]) extends KafkaResponse
object ListTopicsResponse {
  implicit val encoder = deriveEncoder[ListTopicsResponse]
  implicit val decoder = deriveDecoder[ListTopicsResponse]
}

final case class PullLatestResponse(topic: String, keys: Seq[String]) extends KafkaResponse
object PullLatestResponse {
  implicit val encoder = deriveEncoder[PullLatestResponse]
  implicit val decoder = deriveDecoder[PullLatestResponse]
}

/**
  * Rest Requests for debug/admin
  */
sealed trait KafkaSupportRequest
final case class PublishMessage(topic: String, key: String, data: String) extends KafkaSupportRequest
object PublishMessage {
  implicit val decoder = deriveDecoder[PublishMessage]
  implicit val encoder = deriveEncoder[PublishMessage]
}
