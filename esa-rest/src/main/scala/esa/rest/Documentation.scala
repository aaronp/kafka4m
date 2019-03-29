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

  val documentedEndpoints = List(login, currentValue, increment, createUser)

  val api: OpenApi = openApi(
    Info(title = "ESA documentation", version = "1.0.0")
  )(documentedEndpoints: _*)
}
