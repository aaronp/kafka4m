package pipelines.reactive

import io.circe.{Decoder, ObjectEncoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait PipelineRequest
sealed trait PipelineResponse

/**
  * CREATE PIPELINE
  */
case class CreateChainRequest(dataSource: String, transforms: Seq[DataTransform]) extends PipelineRequest
object CreateChainRequest {
  implicit val encoder: ObjectEncoder[CreateChainRequest] = deriveEncoder[CreateChainRequest]
  implicit val decoder: Decoder[CreateChainRequest]       = deriveDecoder[CreateChainRequest]
}
case class CreateChainResponse(pipelineId: String) extends PipelineRequest
object CreateChainResponse {
  implicit val encoder: ObjectEncoder[CreateChainResponse] = deriveEncoder[CreateChainResponse]
  implicit val decoder: Decoder[CreateChainResponse]       = deriveDecoder[CreateChainResponse]
}

/**
  * CONNECT SINK
  */
case class ConnectToSinkRequest(pipelineId: String, dataSourceId: Int, dataSink: String) extends PipelineRequest
object ConnectToSinkRequest {
  implicit val encoder: ObjectEncoder[ConnectToSinkRequest] = deriveEncoder[ConnectToSinkRequest]
  implicit val decoder: Decoder[ConnectToSinkRequest]       = deriveDecoder[ConnectToSinkRequest]
}

case class ConnectToSinkResponse(dataSource: String) extends PipelineResponse
object ConnectToSinkResponse {
  implicit val encoder: ObjectEncoder[ConnectToSinkResponse] = deriveEncoder[ConnectToSinkResponse]
  implicit val decoder: Decoder[ConnectToSinkResponse]       = deriveDecoder[ConnectToSinkResponse]
}
