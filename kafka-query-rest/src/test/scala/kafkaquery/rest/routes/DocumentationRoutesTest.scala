package kafkaquery.rest.routes

import akka.http.scaladsl.testkit.ScalatestRouteTest
import kafkaquery.rest.Documentation
import io.circe._
import io.circe.parser._
import org.scalatest.{Matchers, WordSpec}

class DocumentationRoutesTest extends WordSpec with Matchers with ScalatestRouteTest {

  "DocumentationRoutes.route" should {
    "serve docs" in {
      Get("/openapi.json") ~> DocumentationRoutes.route ~> check {
        val docs        = responseAs[String]
        val Right(json) = decode[Json](docs)
        withClue(json.spaces4) {
          val paths = json.hcursor.downField("paths")

          val Some(keys) = paths.keys.map(_.toList)
          val cs         = Documentation.documentedEndpoints.map(_.path)

          keys should not be (empty)
          keys should contain theSameElementsAs (cs)
        }
      }
    }
  }
}
