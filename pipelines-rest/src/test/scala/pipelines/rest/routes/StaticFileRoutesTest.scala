package pipelines.rest.routes

class StaticFileRoutesTest extends BaseRoutesTest{

  "StaticFileRoutes.route" should {
    "redirect to /index from the root" in {
      val route = StaticFileRoutes.dev()
      Get("/") ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/pipelines.css\">")
      }
    }
    "redirect to /index from the normal path" in {
      val route = StaticFileRoutes.dev()
      Get() ~> route.route ~> check {
        val indexhtml = responseAs[String]
        indexhtml should include("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/pipelines.css\">")
      }
    }
    "serve css routes" in {
      val route = StaticFileRoutes.dev()
      Get("/css/pipelines.css") ~> route.route ~> check {
        val cssContent = responseAs[String]
        cssContent should include("background-color")
      }
    }

    // this fails if we haven't run 'fastOptJS' or 'fullOptJS' on the xhr project
    import eie.io._
    StaticFileRoutes.dev().jsRootDir.asPath.findFirst(8)(_.fileName.matches("pipelines-client-xhr.*opt.*js")).foreach { jsFile =>
      "serve js routes" in {
        val route = StaticFileRoutes.dev()
        Get(s"/js/${jsFile.fileName}") ~> route.route ~> check {
          val javascript = responseAs[String]
          javascript should include("function()")
        }
      }
    }
  }
}
