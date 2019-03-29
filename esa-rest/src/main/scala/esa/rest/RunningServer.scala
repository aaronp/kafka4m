package esa.rest

import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import esa.rest.routes.{EsaRoutes, StaticFileRoutes}
import esa.rest.ssl.{HttpsUtil, SslConfig}

import scala.concurrent.Future
import scala.util.Try

class RunningServer private (val settings: Settings, bindingFuture: Future[Http.ServerBinding])

object RunningServer {

  def dev(settings: Settings) = {

    import settings.implicits._
    // TODO - check the schema in the route, not offer a different port ... perhaps
    val httpBindingFuture = {
      val httpRoutes: Route = EsaRoutes.setupRoutes(StaticFileRoutes.dev.http())
      Http().bindAndHandle(httpRoutes, settings.host, settings.port + 1)
    }
    new RunningServer(settings, httpBindingFuture)
  }

  def setup(settings: Settings): RunningServer = {

    import settings.implicits._
    // TODO - check the schema in the route, not offer a different port ... perhaps
    val httpBindingFuture = {
      val httpRoutes: Route = EsaRoutes.setupRoutes(StaticFileRoutes.fromRootConfig(settings.rootConfig))

      Http().bindAndHandle(httpRoutes, settings.host, settings.port)
    }
    new RunningServer(settings, httpBindingFuture)
  }

  def apply(settings: Settings, sslConf: SslConfig) = {
    import settings.implicits._
    val httpsRoutes: Route            = makeRoutes
    val https: HttpsConnectionContext = loadHttps(sslConf).get
    val httpsBindingFuture            = Http().bindAndHandle(httpsRoutes, settings.host, settings.port, connectionContext = https)
    val bindingFuture                 = httpsBindingFuture
    new RunningServer(settings, bindingFuture)
  }

  def makeRoutes(implicit routingSettings: RoutingSettings): Route = {
    val route = EsaRoutes.https(StaticFileRoutes.dev())
    Route.seal(route)
  }

  def loadHttps(sslConfig: SslConfig): Try[HttpsConnectionContext] = {
    sslConfig.withP12Certificate {
      case (pw, seed, is) => HttpsUtil(pw, seed, is, sslConfig.keystoreType)
    }
  }
}
