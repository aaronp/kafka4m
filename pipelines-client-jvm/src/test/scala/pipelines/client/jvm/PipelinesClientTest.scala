package pipelines.client.jvm

import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{Id, Method, StringBody}
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import pipelines.users.{LoginRequest, LoginResponse}

class PipelinesClientTest extends WordSpec with Matchers with ScalaFutures {

  "PipelinesClient" should {
    "work" in {

      implicit val backend: SttpBackendStub[Id, Nothing] = SttpBackendStub.synchronous
        .whenRequestMatches {
          case request if request.method == Method.POST && request.uri.toString() == "https://localhost:80/users/login" =>
            request.body match {
              case StringBody(json, _, _) =>
                decode[LoginRequest](json) match {
                  case Right(LoginRequest("admin", "correct")) => true
                  case _                                       => false
                }
              case _ => false
            }
        }
        .thenRespond(LoginResponse(true, Option("a.b.c"), None).asJson.noSpaces)
        .whenRequestMatches {
          case request if request.method == Method.POST && request.uri.toString() == "https://localhost:80/users/login" => true
        }
        .thenRespond(LoginResponse(false, None, None).asJson.noSpaces)
        .whenAnyRequest
        .thenRespondNotFound()

      val client = PipelinesClient("https://localhost:80")
      client.login(LoginRequest("admin", "correct")) shouldBe LoginResponse(true, Some("a.b.c"), None)
      client.login(LoginRequest("admin", "wrong")) shouldBe LoginResponse(false, None, None)

    }
  }
}
