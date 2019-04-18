package pipelines.admin

import pipelines.core.{BaseEndpoint, GenericMessageResult}

trait AdminEndpoints extends BaseEndpoint {

  def adminEndpoints(implicit req1: JsonRequest[GenerateServerCertRequest],
                     req2: JsonRequest[UpdateServerCertRequest],
                     resp1: JsonResponse[GenericMessageResult],
                     resp2: JsonResponse[GenerateServerCertResponse],
                     req4: JsonRequest[SetJWTSeedRequest]) = List(
    generate.generateEndpoint,
    updatecert.updateEndpoint,
    seed.seedEndpoint
  )

  /** Generate a server certificate
    */
  object generate {
    def request(implicit req: JsonRequest[GenerateServerCertRequest]): Request[GenerateServerCertRequest] = {
      post(path / "admin" / "gen-cert", jsonRequest[GenerateServerCertRequest](Option("Generate a server certificate and save it")))
    }
    def response(implicit resp: JsonResponse[GenerateServerCertResponse]): Response[GenerateServerCertResponse] =
      jsonResponse[GenerateServerCertResponse](Option("returns the contents of the new certificate"))

    def generateEndpoint(implicit req: JsonRequest[GenerateServerCertRequest],
                         resp: JsonResponse[GenerateServerCertResponse]): Endpoint[GenerateServerCertRequest, GenerateServerCertResponse] = endpoint(request, response)
  }

  /**
    * Replace a server certificate
    */
  object updatecert {
    def request(implicit req: JsonRequest[UpdateServerCertRequest]): Request[UpdateServerCertRequest] = {
      post(path / "admin" / "update-cert", jsonRequest[UpdateServerCertRequest](Option("Updates a server certificate")))
    }
    def updateEndpoint(implicit req: JsonRequest[UpdateServerCertRequest], resp: JsonResponse[GenericMessageResult]): Endpoint[UpdateServerCertRequest, GenericMessageResult] =
      endpoint(request, genericMessageResponse)
  }

  object seed {
    def request(implicit req: JsonRequest[SetJWTSeedRequest]): Request[SetJWTSeedRequest] = {
      post(
        path / "admin" / "update-seed",
        jsonRequest[SetJWTSeedRequest](Option("Sets the 'seed' used for the JWT certificates. Any existing Json Web Tokens will be invalid once this seed is set"))
      )
    }
    def seedEndpoint(implicit req: JsonRequest[SetJWTSeedRequest],
                     resp: JsonResponse[GenericMessageResult]): Endpoint[SetJWTSeedRequest, GenericMessageResult] = endpoint(request, genericMessageResponse)
  }
}
