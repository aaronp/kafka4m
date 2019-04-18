package pipelines.kafka
import java.time

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.java8.time.{JavaTimeDecoders, JavaTimeEncoders}
import pipelines.admin.UpdateServerCertRequest

import scala.concurrent.duration._

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
  * How should a subscriber consume the data?
  *
  * e.g., consider a subscriber who wants 100 data points/second, but only requests more elements at a rate of 10/second?
  *
  * Should we send each second (e.g. the first second's worth of 100 data points will take 10 seconds. On the 11th second, should
  * we send the subsequent second's 100 data points, or jump to the 'latest')?
  *
  */
sealed class StreamStrategy(val name: String)

object StreamStrategy {
  final case object Latest extends StreamStrategy("latest")
  final case object All    extends StreamStrategy("all")

  lazy val values: List[StreamStrategy] = List(Latest, All)
  implicit object StrategyEncoder extends io.circe.Encoder[StreamStrategy] {
    override def apply(a: StreamStrategy): Json = {
      Json.fromString(a.name)
    }
  }
  implicit object StrategyDecoder extends io.circe.Decoder[StreamStrategy] {
    override def apply(c: HCursor): Result[StreamStrategy] = {
      c.as[String].flatMap { name =>
        values.find(_.name == name) match {
          case Some(value) => Right(value)
          case None        => Left(DecodingFailure(s"Expected one of ${values.map(_.name).mkString(",")} but got $name", c.history))
        }
      }
    }
  }
}

final case class Rate(messages: Int, per: FiniteDuration) {
  require(messages > 0, s"rate must specify > 0 messages: $messages")
}

object Rate extends JavaTimeEncoders {
  def perSecond(messages: Int) = Rate(messages, 1.second)

  implicit val rateEncoder = Encoder.instance[Rate] { r8 =>
    val jDuration = java.time.Duration.ofMillis(r8.per.toMillis)
    Json.obj("messages" -> Json.fromInt(r8.messages), "per" -> encodeDuration(jDuration))
  }

  implicit object RateDecoder extends Decoder[Rate] with JavaTimeDecoders {
    override def apply(c: HCursor): Result[Rate] = {
      val perResult: Result[time.Duration] = c.downField("per") match {
        case per: HCursor => decodeDuration(per)
        case other        => Left(DecodingFailure("no 'per' field", other.history))
      }

      for {
        msgs     <- c.downField("messages").as[Int]
        perJTime <- perResult
        per = perJTime.toMillis.millis
      } yield {
        Rate(msgs, per)
      }
    }
  }
}

final case class ResponseFormat(fields: Seq[String])

object ResponseFormat {
  implicit val decoder = deriveDecoder[ResponseFormat]
  implicit val encoder = deriveEncoder[ResponseFormat]
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

/** Represents messages coming from a consumer of a data feed
  */
sealed trait StreamingFeedRequest

object StreamingFeedRequest {
  import cats.syntax.functor._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe.{Decoder, Encoder}

  implicit val encodeRequest: Encoder[StreamingFeedRequest] = Encoder.instance[StreamingFeedRequest] {
    case CancelFeedRequest           => Json.fromString("cancel")
    case Heartbeat                   => Json.fromString("heartbeat")
    case inst @ UpdateFeedRequest(_) => inst.asJson
  }
  private class DecodeString[A](value: String, constant: A) extends Decoder[A] {
    override def apply(c: HCursor): Result[A] = {
      c.as[String] match {
        case Right(`value`) => Right(constant)
        case Right(other)   => Left(DecodingFailure(s"Expected '$value' but got '$other'", c.history))
        case Left(err)      => Left(err)
      }
    }
  }
  implicit val decodeCancelFeed: Decoder[CancelFeedRequest.type] = new DecodeString[CancelFeedRequest.type]("cancel", CancelFeedRequest)
  implicit val decodeHeartbeat: Decoder[Heartbeat.type]          = new DecodeString[Heartbeat.type]("heartbeat", Heartbeat)

  implicit val decodeEvent: Decoder[StreamingFeedRequest] =
    List[Decoder[StreamingFeedRequest]](
      Decoder[Heartbeat.type].widen,
      Decoder[UpdateFeedRequest].widen,
      Decoder[CancelFeedRequest.type].widen
    ).reduceLeft(_ or _)
}
final case object Heartbeat                             extends StreamingFeedRequest
final case object CancelFeedRequest                     extends StreamingFeedRequest
final case class UpdateFeedRequest(query: QueryRequest) extends StreamingFeedRequest

/**
  * Rest Requests for debug/admin
  */
sealed trait KafkaSupportRequest
final case class PublishMessage(topic: String, key: String, data: String) extends KafkaSupportRequest
object PublishMessage {
  implicit val decoder = deriveDecoder[PublishMessage]
  implicit val encoder = deriveEncoder[PublishMessage]
}
