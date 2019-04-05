package kafkaquery.rest.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.config.Config
import kafkaquery.rest.Main

object StaticFileRoutes {

  def dev(): StaticFileRoutes = {
    import args4c.implicits._
    fromRootConfig(Array("dev.conf").asConfig(Main.defaultConfig()).resolve())
  }

  /** @param topLevelConfig the top-level config, e.g. the result of calling 'ConfigFactory.load()'
    * @return the StaticFileRoutes
    */
  def fromRootConfig(topLevelConfig: Config): StaticFileRoutes = {
    apply(topLevelConfig.getConfig("kafkaquery.www"))
  }

  /**
    * @param wwwConfig the relative config which contains the static file route entries
    * @return the StaticFileRoutes
    */
  def apply(wwwConfig: Config): StaticFileRoutes = {
    new StaticFileRoutes(
      htmlRootDir = wwwConfig.getString("htmlDir"),
      landingPage = wwwConfig.getString("landingPage"),
      jsRootDir = wwwConfig.getString("jsDir"),
      cssRootDir = wwwConfig.getString("cssDir")
    )
  }
}

/**
  * TODO -
  *
  * $ consider zipping the static resources on disk, and so just serve them up already zipped to client
  * $ ...otherwise zip the results and set the expiry time
  *
  * @param htmlRootDir the directory to otherwise serve up html resources
  * @param landingPage the redirect landing page (e.g. index.html)
  * @param jsRootDir   the directory which will serve the /js artifacts
  * @param cssRootDir  the directory which will serve the /css artifacts
  */
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
