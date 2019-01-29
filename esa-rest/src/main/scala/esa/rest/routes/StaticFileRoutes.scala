package esa.rest.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.util.Properties

object StaticFileRoutes {

  def devHttps: StaticFileRoutes = dev("https", "/index.html")
  def devHttp: StaticFileRoutes  = dev("http", "/setup.html")

  def devDir          = s"${Properties.userDir}/esa-client-xhr"
  def devResourcesDir = s"$devDir/src/main/resources/web"
  def devJsDir        = s"$devDir/target/scala-2.12"
  private def dev(suffix: String, page: String): StaticFileRoutes = {
    StaticFileRoutes(
      htmlRootDir = s"$devResourcesDir/$suffix",
      landingPage = page,
      jsRootDir = devJsDir,
      cssRootDir = s"$devResourcesDir/css"
    )
  }
}

case class StaticFileRoutes(htmlRootDir: String, landingPage: String, jsRootDir: String, cssRootDir: String) {
  require(!htmlRootDir.endsWith("/"), s"htmlRootDir '$htmlRootDir' shouldn't end w/ a forward slash")
  require(!jsRootDir.endsWith("/"), s"jsRootDir '$jsRootDir' shouldn't end w/ a forward slash")
  require(!cssRootDir.endsWith("/"), s"cssRootDir '$cssRootDir' shouldn't end w/ a forward slash")

  def route: Route = {

    jsResource ~ cssResource ~ htmlResource
  }

  private def htmlResource = {
    get {
      (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
        getFromFile(htmlRootDir + landingPage)
      } ~ {
        getFromDirectory(htmlRootDir)
      }
    }
  }

  private def jsResource = {
    (get & pathPrefix("js")) {
      getFromDirectory(jsRootDir)
    }
  }

  private def cssResource = {
    (get & pathPrefix("css")) {
      getFromDirectory(cssRootDir)
    }
  }

}
