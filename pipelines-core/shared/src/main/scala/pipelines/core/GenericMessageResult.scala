package pipelines.core

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class GenericMessageResult(message: String)
object GenericMessageResult {

  implicit val encoder = deriveEncoder[GenericMessageResult]
  implicit val decoder = deriveDecoder[GenericMessageResult]
}
