package pipelines.data

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

sealed trait DataRegistryRequest

object DataRegistryRequest {
  implicit val encodeEvent: Encoder[DataRegistryRequest] = Encoder.instance {
    case request @ Connect(_, _)                => request.asJson
    case request @ FilterSourceRequest(_, _, _) => request.asJson
    case request @ UpdateFilterRequest(_, _)    => request.asJson
    case request @ ChangeTypeRequest(_, _, _)   => request.asJson
  }

  implicit val decodeEvent: Decoder[DataRegistryRequest] =
    List[Decoder[DataRegistryRequest]](
      Decoder[Connect].widen,
      Decoder[UpdateFilterRequest].widen,
      Decoder[FilterSourceRequest].widen,
      Decoder[ChangeTypeRequest].widen
    ).reduceLeft(_ or _)
}

case class Connect(sourceKey: String, sinkKey: String)                                            extends DataRegistryRequest
case class UpdateFilterRequest(sourceKey: String, updatedFilterExpression: String)                extends DataRegistryRequest
case class FilterSourceRequest(sourceKey: String, newSourceKey: String, filterExpression: String) extends DataRegistryRequest
case class ChangeTypeRequest(sourceKey: String, newSourceKey: String, newType: DataType)          extends DataRegistryRequest
