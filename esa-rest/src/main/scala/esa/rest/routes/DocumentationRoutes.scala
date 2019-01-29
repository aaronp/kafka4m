package esa.rest.routes
import akka.http.scaladsl.server.Route
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import endpoints.openapi.model.OpenApi
import esa.rest.Documentation
import io.circe.{Decoder, Encoder}

object DocumentationRoutes extends server.Endpoints with JsonSchemaEntities {

  def route: Route = {
    val docEndpoint: Endpoint[Unit, OpenApi] = {
      implicit val schema: JsonSchema[OpenApi] = {
        import io.circe.generic.auto._
        JsonSchema[OpenApi](Encoder[OpenApi], Decoder[OpenApi])
      }
      endpoint(get(path / "openapi.json"), jsonResponse[OpenApi]())
    }
    docEndpoint.implementedBy(_ => Documentation.api)
  }
}
