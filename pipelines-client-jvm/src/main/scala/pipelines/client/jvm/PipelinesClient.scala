package pipelines.client.jvm

//import akka.actor.ActorSystem
//import akka.http.scaladsl.model.Uri
//import akka.stream.{ActorMaterializer, Materializer}
//import endpoints.akkahttp.client.{AkkaHttpRequestExecutor, Endpoints, EndpointsSettings, JsonEntitiesFromCodec}
import com.softwaremill.sttp
import com.softwaremill.sttp.{SttpBackend, TryHttpURLConnectionBackend}
import endpoints.algebra.{Codec, Documentation}
import endpoints.algebra.circe.CirceCodec
import endpoints.sttp.client.{BasicAuthentication, JsonEntitiesFromCodec}
import pipelines.admin.LoginEndpoints
import pipelines.users.{LoginRequest, LoginResponse}

import scala.util.Try

object ClientEncoder extends endpoints.circe.JsonSchemas {

//  with endpoints.algebra.circe.JsonEntitiesFromCodec
  //with endpoints.algebra.circe.JsonEntitiesFromCodec

  implicit def loginRequestSchema: JsonSchema[LoginRequest]   = JsonSchema(implicitly, implicitly)
  implicit def loginResponseSchema: JsonSchema[LoginResponse] = JsonSchema(implicitly, implicitly)

}

//BasicAuthentication with
class PipelinesClient[R[_]](host: String, backend: sttp.SttpBackend[R, _])
    extends endpoints.sttp.client.Endpoints[R](host, backend)
//    with endpoints.circe.JsonSchemas
    with endpoints.sttp.client.JsonEntitiesFromCodec[R]
    with endpoints.algebra.circe.JsonEntitiesFromCodec
//    with BasicAuthentication[R]
//    with JsonEntitiesFromCodec[R]
    with LoginEndpoints {
  import ClientEncoder._

  def login(login: LoginRequest): R[LoginResponse] = loginEndpoint.apply(login -> None)
}

object PipelinesClient {

  def apply(host: String): PipelinesClient[Try] = {
    val backend: SttpBackend[Try, Nothing] = TryHttpURLConnectionBackend()
    new PipelinesClient[Try](host, backend)
  }

}
