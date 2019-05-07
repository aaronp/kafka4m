package pipelines.core

import endpoints.algebra

trait BaseEndpoint extends algebra.Endpoints with algebra.JsonEntities {
  def genericMessageResponse(implicit resp: JsonResponse[GenericMessageResult]): Response[GenericMessageResult] = jsonResponse[GenericMessageResult](Option("A response which contains some general information message"))
}
