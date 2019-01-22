package esa.rest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe._
import io.circe.parser._
import org.scalatest.{Matchers, WordSpec}

class DocumentationServerTest extends WordSpec with Matchers with ScalatestRouteTest {

  "DocumentationServer.route" should {
    "server docs" in {
      Get("/openapi.json") ~> DocumentationServer.route ~> check {
        val docs        = responseAs[String]
        val Right(json) = decode[Json](docs)
        withClue(json.spaces4) {
          val paths = json.hcursor.downField("paths")

          val Some(keys) = paths.keys.map(_.toList)
          keys should contain only ("/current-value", "/increment")
        }
      }
    }
  }
}
