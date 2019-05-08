package pipelines.core

import cats.syntax.functor._
import io.circe.{Decoder, Encoder, Json}

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

final case object Heartbeat         extends StreamingRequest
final case object CancelFeedRequest extends StreamingRequest
