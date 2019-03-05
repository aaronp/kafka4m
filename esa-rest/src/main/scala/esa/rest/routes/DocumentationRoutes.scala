package esa.rest.routes
import akka.http.scaladsl.server.Route
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import endpoints.openapi.model.OpenApi
import esa.rest.Documentation
import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor}

object DocumentationRoutes extends server.Endpoints with JsonSchemaEntities {

  def route: Route = {
    val docEndpoint: Endpoint[Unit, OpenApi] = {
      implicit val schema: JsonSchema[OpenApi] = {
        object openApiDecoder extends Decoder[OpenApi] {
          override def apply(c: HCursor): Result[OpenApi] = {
            Left(DecodingFailure("not supported", c.history))
          }
        }
//          import io.circe.generic.auto._
//          Decoder[OpenApi]
//        val openApiDecoder: Decoder[OpenApi] = {
//          import io.circe.generic.auto._
//          Decoder[OpenApi]
//        }
        JsonSchema[OpenApi](OpenApi.jsonEncoder, openApiDecoder)

      }
      endpoint(get(path / "openapi.json"), jsonResponse[OpenApi]())
    }
    docEndpoint.implementedBy(_ => Documentation.api)
  }
}
