package pipelines.stream

import io.circe.Json
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class RegisteredSource(name: String, dataType: String)
object RegisteredSource {
  implicit val encoder = deriveEncoder[RegisteredSource]
  implicit val decoder = deriveDecoder[RegisteredSource]
}

case class ListSourceResponse(sources: Seq[RegisteredSource])
object ListSourceResponse {
  implicit val encoder = deriveEncoder[ListSourceResponse]
  implicit val decoder = deriveDecoder[ListSourceResponse]
}

case class PeekResponse(data: Json)
object PeekResponse {
  implicit val encoder = deriveEncoder[PeekResponse]
  implicit val decoder = deriveDecoder[PeekResponse]
}
