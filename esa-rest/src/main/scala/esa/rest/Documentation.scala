package esa.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}
import esa.endpoints.{CounterEndpoints, UserEndpoints}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation //
    extends openapi.Endpoints       //
    with openapi.JsonSchemaEntities //
    with CounterEndpoints //
    with UserEndpoints {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(login, currentValue, increment)
}
