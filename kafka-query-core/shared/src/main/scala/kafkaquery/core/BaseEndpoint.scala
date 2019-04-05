package kafkaquery.core

import endpoints.algebra
import io.circe.generic.auto._

trait BaseEndpoint extends algebra.Endpoints with algebra.JsonSchemaEntities with endpoints.circe.JsonSchemas  {

  def genericMessageResponse: Response[GenericMessageResult] = jsonResponse[GenericMessageResult](Option("A response which contains some general information message"))

  implicit lazy val `JsonSchema[GenericMessageResult]`: JsonSchema[GenericMessageResult]   = JsonSchema(implicitly, implicitly)
}
