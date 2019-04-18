package pipelines.rest.routes
import akka.http.scaladsl.server.Route
import endpoints.openapi.model.{OpenApi, OpenApiSchemas}
import pipelines.rest.openapi.Documentation

import com.typesafe.scalalogging.StrictLogging
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import pipelines.core.GenericMessageResult

//with endpoints.circe.JsonSchemas with StrictLogging
object DocumentationRoutes extends OpenApiSchemas with server.Endpoints with JsonSchemaEntities {

  def route: Route = {
    val docEndpoint: Endpoint[Unit, OpenApi] = {
      endpoint(get(path / "openapi.json"), jsonResponse[OpenApi]())
    }
    docEndpoint.implementedBy(_ => Documentation.api)
  }
}
