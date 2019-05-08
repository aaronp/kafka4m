package pipelines.data

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

sealed trait DataRegistryRequest

object DataRegistryRequest {
  implicit val encoder: Encoder[DataRegistryRequest] = Encoder.instance {
    case request @ Connect(_, _)                => request.asJson
    case request @ ModifySourceRequest(_, _, _) => request.asJson
    case request @ UpdateSourceRequest(_, _)    => request.asJson
  }

  implicit val decoder: Decoder[DataRegistryRequest] =
    List[Decoder[DataRegistryRequest]](
      Decoder[Connect].widen,
      Decoder[ModifySourceRequest].widen,
      Decoder[UpdateSourceRequest].widen
    ).reduceLeft(_ or _)
}

case class Connect(sourceKey: String, sinkKey: String) extends DataRegistryRequest

case class ModifySourceRequest(sourceKey: String, newSourceKey: String, enrichment: ModifyObservableRequest) extends DataRegistryRequest
case class UpdateSourceRequest(sourceKey: String, enrichment: ModifyObservableRequest)                       extends DataRegistryRequest
