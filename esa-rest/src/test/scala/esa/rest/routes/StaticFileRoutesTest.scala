package esa.rest.routes

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class StaticFileRoutesTest extends WordSpec with Matchers with ScalatestRouteTest {

  "StaticFileRoutes.route" should {
    "redirect to /index from the root" in {
      val route = StaticFileRoutes.devHttps
      Get("/") ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/esa.css\">")
      }
    }
    "redirect to /index from the normal path" in {
      val route = StaticFileRoutes.devHttps
      Get() ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/esa.css\">")
      }
    }
    "serve css routes" in {
      val route = StaticFileRoutes.devHttps
      Get("/css/esa.css") ~> route.route ~> check {
        val cssContent = responseAs[String]
        cssContent should include("background-color")
      }
    }

    // this fails if we haven't run 'fastOptJS' on the xhr project
    import eie.io._
    StaticFileRoutes.devJsDir.asPath.findFirst(8)(_.fileName == "esa-client-xhr-fastopt.js").foreach { _ =>
      "serve js routes" in {
        val route = StaticFileRoutes.devHttps
        Get("/js/esa-client-xhr-fastopt.js") ~> route.route ~> check {
          val javascript = responseAs[String]
          javascript should include("The top-level Scala.js environment")
        }
      }
    }
  }
}
