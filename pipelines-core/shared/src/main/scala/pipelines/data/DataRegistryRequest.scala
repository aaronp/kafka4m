package pipelines.data

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import pipelines.core.Enrichment

sealed trait DataRegistryRequest

object DataRegistryRequest {
  implicit val encoder: Encoder[DataRegistryRequest] = Encoder.instance {
    case request @ Connect(_, _)                     => request.asJson
    case request @ EnrichSourceRequest(_, _, _)      => request.asJson
    case request @ UpdateEnrichedSourceRequest(_, _) => request.asJson
  }

  implicit val decoder: Decoder[DataRegistryRequest] =
    List[Decoder[DataRegistryRequest]](
      Decoder[Connect].widen,
      Decoder[EnrichSourceRequest].widen,
      Decoder[UpdateEnrichedSourceRequest].widen
    ).reduceLeft(_ or _)
}

case class Connect(sourceKey: String, sinkKey: String) extends DataRegistryRequest

case class EnrichSourceRequest(sourceKey: String, newSourceKey: String, enrichment: Enrichment) extends DataRegistryRequest
case class UpdateEnrichedSourceRequest(sourceKey: String, enrichment: Enrichment)               extends DataRegistryRequest
