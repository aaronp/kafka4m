package esa.rest.routes
import akka.http.scaladsl.server.Route
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import endpoints.openapi.model.{OpenApi, OpenApiSchemas}
import esa.rest.Documentation

object DocumentationRoutes extends server.Endpoints with OpenApiSchemas with JsonSchemaEntities {

  def route: Route = {
    val docEndpoint: Endpoint[Unit, OpenApi] = {
      endpoint(get(path / "openapi.json"), jsonResponse[OpenApi]())
    }
    docEndpoint.implementedBy(_ => Documentation.api)
  }
}
