package pipelines.core

import endpoints.algebra
import io.circe.generic.auto._
import pipelines.admin.GenerateServerCertResponse

trait BaseEndpoint extends algebra.Endpoints  with algebra.JsonEntities { //with algebra.JsonEntitiesFromCodec {
  //with algebra.JsonSchemaEntities
//trait BaseEndpoint extends algebra.Endpoints with endpoints.algebra.CirceEntities with endpoints.circe.JsonSchemas  {

//  def genericMessageResponse(implicit resp: JsonResponse[GenericMessageResult]): Response[GenericMessageResult] = jsonResponse[GenericMessageResult](Option("A response which contains some general information message"))

//  implicit lazy val `JsonSchema[GenericMessageResult]`: JsonSchema[GenericMessageResult]   = JsonSchema(implicitly, implicitly)
}
