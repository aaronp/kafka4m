package kafkaquery.endpoints


sealed trait AdminRequest
sealed trait AdminResponse

final case class GenerateServerCertRequest(saveToPath : String) extends AdminRequest
final case class GenerateServerCertResponse(certificate : String) extends AdminResponse

final case class UpdateServerCertRequest(certificate : String, saveToPath : String) extends AdminRequest

final case class SetJWTSeedRequest(seed : String) extends AdminRequest
