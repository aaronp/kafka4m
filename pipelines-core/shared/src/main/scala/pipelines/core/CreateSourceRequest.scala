package pipelines.core

import io.circe.Json

sealed trait CreateSourceRequest

object CreateSourceRequest {

}

case class CreateAvroSource(schema: String)         extends CreateSourceRequest
case class CreateKafkaSource(kafkaProperties: Json) extends CreateSourceRequest
case class CreateJsonSource()                       extends CreateSourceRequest
