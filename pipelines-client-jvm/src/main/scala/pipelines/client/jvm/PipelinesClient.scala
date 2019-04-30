package pipelines.client.jvm

import com.softwaremill.sttp
import com.softwaremill.sttp.{SttpBackend, TryHttpURLConnectionBackend}
import endpoints.algebra.{Codec, Documentation, JsonSchemaEntities}
import pipelines.admin.LoginEndpoints
import pipelines.users.{LoginRequest, LoginResponse}

import scala.util.Try

//object ClientEncoder extends endpoints.circe.JsonSchemas {
//
////  with endpoints.algebra.circe.JsonEntitiesFromCodec
//  //with endpoints.algebra.circe.JsonEntitiesFromCodec
//
//  implicit def loginRequestSchema: JsonSchema[LoginRequest]   = JsonSchema(implicitly, implicitly)
//  implicit def loginResponseSchema: JsonSchema[LoginResponse] = JsonSchema(implicitly, implicitly)
//
//  def loginC : endpoints.algebra.Codec[String, LoginRequest] = null
//
//}

//BasicAuthentication with
class PipelinesClient[R[_]](host: String, backend: sttp.SttpBackend[R, _])
    extends endpoints.sttp.client.Endpoints[R](host, backend)
    with endpoints.algebra.circe.JsonEntitiesFromCodec
    with endpoints.circe.JsonSchemas
//    with endpoints.sttp.client.JsonEntitiesFromCodec[R]
//    with endpoints.algebra.circe.JsonEntitiesFromCodec
//    with BasicAuthentication[R]
//    with JsonEntitiesFromCodec[R]
    with LoginEndpoints {

//  def wtf: JsonSchemaEntities = this

  implicit def loginRequestSchema: JsonSchema[LoginRequest]   = JsonSchema(implicitly, implicitly)
  implicit def loginResponseSchema: JsonSchema[LoginResponse] = JsonSchema(implicitly, implicitly)

  type X[A] = JsonRequest[A]

  def login(login: LoginRequest): R[LoginResponse] = loginEndpoint.apply(login -> None)

//  override def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): RequestEntity[A] = { (a, req) =>
//    req.body(codec.encode(a)).contentType("application/json")
//  }
//
//  override def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]): Response[A] = new SttpResponse[A] {
//    override type ReceivedBody = Either[Exception, A]
//    override def responseAs = sttp.asString.map(str => codec.decode(str))
//    override def validateResponse(response: sttp.Response[ReceivedBody]): R[A] = {
//      response.unsafeBody match {
//        case Right(a) => backend.responseMonad.unit(a)
//        case Left(exception) => backend.responseMonad.error(exception)
//      }
//    }
//  }
  override def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): (A, SttpRequest) => SttpRequest = { (a, req) =>
    req.body(codec.encode(a)).contentType("application/json")
  }

  override def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]): SttpResponse[A] = new SttpResponse[A] {
    override type ReceivedBody = Either[Exception, A]
    override def responseAs = sttp.asString.map(str => codec.decode(str))
    override def validateResponse(response: sttp.Response[ReceivedBody]): R[A] = {
      response.unsafeBody match {
        case Right(a)        => backend.responseMonad.unit(a)
        case Left(exception) => backend.responseMonad.error(exception)
      }
    }
  }
}

object PipelinesClient {

  def apply(host: String): PipelinesClient[Try] = {
    val backend: SttpBackend[Try, Nothing] = TryHttpURLConnectionBackend()
    new PipelinesClient[Try](host, backend)
  }

}
