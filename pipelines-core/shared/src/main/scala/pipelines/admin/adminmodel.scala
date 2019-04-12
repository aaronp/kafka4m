package pipelines.admin

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait AdminRequest
sealed trait AdminResponse

final case class GenerateServerCertRequest(saveToPath: String) extends AdminRequest

object GenerateServerCertRequest {
  implicit val encoder = deriveEncoder[GenerateServerCertRequest]
  implicit val decoder = deriveDecoder[GenerateServerCertRequest]
}
final case class GenerateServerCertResponse(certificate: String) extends AdminResponse

object GenerateServerCertResponse {
  implicit val encoder = deriveEncoder[GenerateServerCertResponse]
  implicit val decoder = deriveDecoder[GenerateServerCertResponse]
}

final case class UpdateServerCertRequest(certificate: String, saveToPath: String) extends AdminRequest

object UpdateServerCertRequest {
  implicit val encoder = deriveEncoder[UpdateServerCertRequest]
  implicit val decoder = deriveDecoder[UpdateServerCertRequest]
}

final case class SetJWTSeedRequest(seed: String) extends AdminRequest

object SetJWTSeedRequest {
  implicit val encoder = deriveEncoder[SetJWTSeedRequest]
  implicit val decoder = deriveDecoder[SetJWTSeedRequest]
}
