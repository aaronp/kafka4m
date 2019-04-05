package esa.endpoints

import endpoints.{algebra, generic}

trait BaseEndpoint extends algebra.Endpoints with algebra.JsonSchemaEntities with generic.JsonSchemas {

  def genericMessageResponse: Response[GenericMessageResult] = jsonResponse[GenericMessageResult](Option("A response which contains some general information message"))

  implicit lazy val `JsonSchema[GenericMessageResult]`: JsonSchema[GenericMessageResult]   = genericJsonSchema
}
