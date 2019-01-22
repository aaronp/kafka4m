package esa.rest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class StaticFileServerTest extends WordSpec with Matchers with ScalatestRouteTest {

  "StaticFileServer.route" should {
    "redirect to /index from the root" in {
      val route = StaticFileServer.dev
      Get("/") ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/esa.css\">")
      }
    }
    "redirect to /index from the normal path" in {
      val route = StaticFileServer.dev
      Get() ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/esa.css\">")
      }
    }
    "serve css routes" in {
      val route = StaticFileServer.dev
      Get("/css/esa.css") ~> route.route ~> check {
        val cssContent = responseAs[String]
        cssContent should include("background-color")
      }
    }
    "serve js routes" in {
      val route = StaticFileServer.dev
      Get("/js/esa-client-xhr-fastopt.js") ~> route.route ~> check {
        val javascript = responseAs[String]
        javascript should include("The top-level Scala.js environment")
      }
    }
  }
}
