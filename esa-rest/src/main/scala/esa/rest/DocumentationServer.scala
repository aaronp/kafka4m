package esa.rest
import akka.http.scaladsl.server.Route
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import endpoints.openapi.model.OpenApi
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}

object DocumentationServer extends server.Endpoints with JsonSchemaEntities {

  def route: Route = {
    val docEndpoint: Endpoint[Unit, OpenApi] = {
      implicit val schema: JsonSchema[OpenApi] = JsonSchema[OpenApi](Encoder[OpenApi], Decoder[OpenApi])
      endpoint(get(path / "openapi.json"), jsonResponse[OpenApi]())
    }
    docEndpoint.implementedBy(_ => CounterDocumentation.api)
  }
}
