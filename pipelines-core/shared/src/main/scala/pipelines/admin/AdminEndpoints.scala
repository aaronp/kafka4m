package pipelines.admin

import pipelines.core.BaseEndpoint
import pipelines.core.GenericMessageResult
import io.circe.generic.semiauto._

trait AdminEndpoints extends BaseEndpoint {

  def adminEndpoints = List(
    generate.generateEndpoint,
    updatecert.updateEndpoint,
    seed.seedEndpoint
  )

  /** Generate a server certificate
    */
  object generate {
    def request: Request[GenerateServerCertRequest] = {
      post(path / "admin" / "gen-cert", jsonRequest[GenerateServerCertRequest](Option("Generate a server certificate and save it")))
    }
    def response: Response[GenerateServerCertResponse] = jsonResponse[GenerateServerCertResponse](Option("returns the contents of the new certificate"))

    implicit lazy val requestSchema : JsonSchema[GenerateServerCertRequest]   = JsonSchema(implicitly, implicitly)
    implicit lazy val responseSchema : JsonSchema[GenerateServerCertResponse] = JsonSchema(implicitly, implicitly)

    val generateEndpoint: Endpoint[GenerateServerCertRequest, GenerateServerCertResponse] = endpoint(request, response)
  }

  /**
    * Replace a server certificate
    */
  object updatecert {
    def request: Request[UpdateServerCertRequest] = {
      post(path / "admin" / "update-cert", jsonRequest[UpdateServerCertRequest](Option("Updates a server certificate")))
    }
    implicit lazy val requestSchema : JsonSchema[UpdateServerCertRequest] = JsonSchema(implicitly, implicitly)
    val updateEndpoint: Endpoint[UpdateServerCertRequest, GenericMessageResult] = endpoint(request, genericMessageResponse)
  }

  object seed {
    def request: Request[SetJWTSeedRequest] = {
      post(
        path / "admin" / "update-seed",
        jsonRequest[SetJWTSeedRequest](Option("Sets the 'seed' used for the JWT certificates. Any existing Json Web Tokens will be invalid once this seed is set"))
      )
    }
    implicit lazy val schema: JsonSchema[SetJWTSeedRequest] = JsonSchema(deriveEncoder[SetJWTSeedRequest], deriveDecoder[SetJWTSeedRequest])

    val seedEndpoint: Endpoint[SetJWTSeedRequest, GenericMessageResult] = endpoint(request, genericMessageResponse)
  }
}
