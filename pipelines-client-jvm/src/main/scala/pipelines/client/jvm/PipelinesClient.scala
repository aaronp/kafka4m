package pipelines.client.jvm

import java.net.HttpURLConnection

import com.softwaremill.sttp
import com.softwaremill.sttp.{SttpBackend, TryHttpURLConnectionBackend}
import com.typesafe.config.ConfigFactory
import javax.net.ssl.{HttpsURLConnection, SSLContext}
import pipelines.admin.LoginEndpoints
import pipelines.ssl.SSLConfig
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

    val ctxt: SSLContext = {
      val preparedConf = ConfigFactory.parseString("""
        |pipelines.tls.certificate=/Users/aaronpritzlaff/dev/sandbox/pipelines/target/certificates/cert.p12
        |pipelines.tls.seed=foo
        |pipelines.tls.password=password
      """.stripMargin).withFallback(ConfigFactory.load())
      val sslConf: SSLConfig = pipelines.ssl.SSLConfig(preparedConf.getConfig("pipelines.tls"))
      sslConf.newContext().get
    }
    val backend: SttpBackend[Try, Nothing] = TryHttpURLConnectionBackend(customizeConnection = {
      case conn: HttpsURLConnection =>
        conn.setSSLSocketFactory(
      case _ =>
    })

    new PipelinesClient[Try](host, backend)
  }

}
