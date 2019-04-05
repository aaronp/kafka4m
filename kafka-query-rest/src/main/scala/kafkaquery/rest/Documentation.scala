package esa.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}
import esa.endpoints.{AdminEndpoints, CounterEndpoints, UserEndpoints}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation //
    extends openapi.Endpoints       //
    with openapi.JsonSchemaEntities //
    with CounterEndpoints //
    with UserEndpoints    //
    with AdminEndpoints   //
    {

  val documentedEndpoints: List[Documentation.DocumentedEndpoint] = {
    userEndpoints ++ adminEndpoints ++ List(currentValue, increment)
  }

  val api: OpenApi = openApi(
    Info(title = "ESA documentation", version = "1.0.0")
  )(documentedEndpoints: _*)
}
