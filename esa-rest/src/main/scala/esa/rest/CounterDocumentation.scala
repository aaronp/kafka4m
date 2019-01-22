package esa.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}
import quickstart.CounterEndpoints

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object CounterDocumentation extends CounterEndpoints with openapi.Endpoints with openapi.JsonSchemaEntities {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, increment)

}
