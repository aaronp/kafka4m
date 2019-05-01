package pipelines.data

import io.circe.{Decoder, Encoder}

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

sealed trait DataRegistryResponse
object DataRegistryResponse {

  implicit val encodeEvent: Encoder[DataRegistryResponse] = Encoder.instance {
    case request @ SourceNotFoundResponse(_)               => request.asJson
    case request @ SinkNotFoundResponse(_)                 => request.asJson
    case request @ SourceSinkMismatchResponse(_, _, _, _)  => request.asJson
    case request @ SourceAlreadyExistsResponse(_)          => request.asJson
    case request @ SourceCreatedResponse(_, _)             => request.asJson
    case request @ UnsupportedTypeMappingResponse(_, _, _) => request.asJson
    case request @ ConnectResponse(_, _)                   => request.asJson
    case request @ ErrorCreatingSource(_, _)               => request.asJson
  }

  implicit val decodeEvent: Decoder[DataRegistryResponse] =
    List[Decoder[DataRegistryResponse]](
      Decoder[SourceNotFoundResponse].widen,
      Decoder[SinkNotFoundResponse].widen,
      Decoder[SourceSinkMismatchResponse].widen,
      Decoder[SourceAlreadyExistsResponse].widen,
      Decoder[SourceCreatedResponse].widen,
      Decoder[UnsupportedTypeMappingResponse].widen,
      Decoder[ConnectResponse].widen,
      Decoder[ErrorCreatingSource].widen
    ).reduceLeft(_ or _)
}

case class SourceNotFoundResponse(missingSourceKey: String)                                                         extends DataRegistryResponse
case class SinkNotFoundResponse(missingSinkKey: String)                                                             extends DataRegistryResponse
case class SourceSinkMismatchResponse(sourceKey: String, sinkKey: String, sourceType: DataType, sinkType: DataType) extends DataRegistryResponse
case class SourceAlreadyExistsResponse(existingSourceKey: String)                                                   extends DataRegistryResponse
case class SourceCreatedResponse(newSourceKey: String, dataType: DataType)                                          extends DataRegistryResponse
case class UnsupportedTypeMappingResponse(sourceKey: String, fromType: DataType, toType: DataType)                  extends DataRegistryResponse
case class ConnectResponse(connectedSourceKey: String, connectedSinkKey: String)                                    extends DataRegistryResponse
case class ErrorCreatingSource(newSourceKey: String, errorMessage: String)                                          extends DataRegistryResponse
