package pipelines.core

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

sealed trait CreateSourceRequest

object CreateSourceRequest {

  implicit val encoder: Encoder[CreateSourceRequest] = Encoder.instance {
    case request @ CreateAvroSource(_)  => request.asJson
    case request @ CreateKafkaSource(_) => request.asJson
    case request @ CreateJsonSource(_)  => request.asJson
  }

  implicit val decoder: Decoder[CreateSourceRequest] =
    List[Decoder[CreateSourceRequest]](
      Decoder[CreateAvroSource].widen,
      Decoder[CreateKafkaSource].widen,
      Decoder[CreateJsonSource].widen
    ).reduceLeft(_ or _)
}

case class CreateAvroSource(schema: String)                    extends CreateSourceRequest
case class CreateKafkaSource(kafkaProperties: Json)            extends CreateSourceRequest
case class CreateJsonSource(jsonSchema: Option[String] = None) extends CreateSourceRequest
