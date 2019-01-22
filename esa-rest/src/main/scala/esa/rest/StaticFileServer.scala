package esa.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

object StaticFileServer {

  def dev = {
    val dir          = "/Users/aaron/dev/playground/esa/esa-client-xhr"
    val resourcesDir = s"$dir/src/main/resources/web"
    StaticFileServer(
      htmlRootDir = s"$resourcesDir",
      jsRootDir = s"$dir/target/scala-2.12",
      cssRootDir = s"$resourcesDir/css"
    )
  }
}

case class StaticFileServer(htmlRootDir: String, jsRootDir: String, cssRootDir: String) {
  require(!htmlRootDir.endsWith("/"), s"htmlRootDir '$htmlRootDir' shouldn't end w/ a forward slash")
  require(!jsRootDir.endsWith("/"), s"jsRootDir '$jsRootDir' shouldn't end w/ a forward slash")
  require(!cssRootDir.endsWith("/"), s"cssRootDir '$cssRootDir' shouldn't end w/ a forward slash")

  def route: Route = {
    jsResource ~ cssResource ~ htmlResource
  }

  private def htmlResource = {
    get {
      (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
        getFromFile(htmlRootDir + "/index.html")
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
