package pipelines.client.jvm

import com.softwaremill.sttp
import com.softwaremill.sttp.{SttpBackend, TryHttpURLConnectionBackend}
import pipelines.admin.LoginEndpoints
import pipelines.users.{LoginRequest, LoginResponse}

import scala.util.Try

class PipelinesClient[R[_]](host: String, backend: sttp.SttpBackend[R, _])
    extends endpoints.sttp.client.Endpoints[R](host, backend)
    with endpoints.algebra.circe.JsonEntitiesFromCodec
    with endpoints.circe.JsonSchemas
    with endpoints.sttp.client.JsonEntitiesFromCodec[R]
    with LoginEndpoints {

  implicit def loginRequestSchema: JsonSchema[LoginRequest]   = JsonSchema(implicitly, implicitly)
  implicit def loginResponseSchema: JsonSchema[LoginResponse] = JsonSchema(implicitly, implicitly)

  def login(login: LoginRequest): R[LoginResponse] = loginEndpoint.apply(login -> None)

}

object PipelinesClient {

  def apply(host: String): PipelinesClient[Try] = {
    val backend: SttpBackend[Try, Nothing] = TryHttpURLConnectionBackend()
    new PipelinesClient[Try](host, backend)
  }

}
