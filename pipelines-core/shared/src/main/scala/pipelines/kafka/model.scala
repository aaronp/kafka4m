package pipelines.kafka

import cats.syntax.functor._
import io.circe.Decoder.Result
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json, _}
import pipelines.core.{Rate, StreamStrategy}

private class DecodeString[A](value: String, constant: A) extends Decoder[A] {
  override def apply(c: HCursor): Result[A] = {
    c.as[String] match {
      case Right(`value`) => Right(constant)
      case Right(other)   => Left(DecodingFailure(s"Expected '$value' but got '$other'", c.history))
      case Left(err)      => Left(err)
    }
  }
}


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

final case object Heartbeat                             extends StreamingFeedRequest with StreamingRequest
final case object CancelFeedRequest                     extends StreamingFeedRequest with StreamingRequest
final case class UpdateFeedRequest(query: QueryRequest) extends StreamingFeedRequest
object UpdateFeedRequest {
  implicit def decoder = deriveDecoder[UpdateFeedRequest]
  implicit def encoder: ObjectEncoder[UpdateFeedRequest] = deriveEncoder[UpdateFeedRequest]
}

/**
  * A StreamingRequest is only the heartbeat/cancel components of a StreamingFeedRequest
  */
sealed trait StreamingRequest
object StreamingRequest {
  implicit val encodeRequest: Encoder[StreamingRequest] = Encoder.instance[StreamingRequest] {
    case CancelFeedRequest => Json.fromString("cancel")
    case Heartbeat         => Json.fromString("heartbeat")
  }

  implicit val decodeCancelFeed: Decoder[CancelFeedRequest.type] = new DecodeString[CancelFeedRequest.type]("cancel", CancelFeedRequest)
  implicit val decodeHeartbeat: Decoder[Heartbeat.type]          = new DecodeString[Heartbeat.type]("heartbeat", Heartbeat)

  implicit val decodeEvent: Decoder[StreamingRequest] =
    List[Decoder[StreamingRequest]](
      Decoder[Heartbeat.type].widen,
      Decoder[CancelFeedRequest.type].widen
    ).reduceLeft(_ or _)
}

/** Represents messages coming from a consumer of a data feed
  */
sealed trait StreamingFeedRequest

object StreamingFeedRequest {

  implicit val encodeRequest: Encoder[StreamingFeedRequest] = Encoder.instance[StreamingFeedRequest] {
    case CancelFeedRequest           => StreamingRequest.encodeRequest(CancelFeedRequest)
    case Heartbeat                   => StreamingRequest.encodeRequest(Heartbeat)
    case inst @ UpdateFeedRequest(_) => inst.asJson
  }

  implicit val decodeEvent: Decoder[StreamingFeedRequest] =
    List[Decoder[StreamingFeedRequest]](
      Decoder[Heartbeat.type].widen,
      Decoder[UpdateFeedRequest].widen, //      Decoder[UpdateFeedRequest[_]].widen,
      Decoder[CancelFeedRequest.type].widen
    ).reduceLeft(_ or _)
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
