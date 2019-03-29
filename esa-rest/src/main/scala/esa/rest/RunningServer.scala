package esa.rest

import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import esa.rest.routes.{EsaRoutes, StaticFileRoutes}
import esa.rest.ssl.{HttpsUtil, SslConfig}

import scala.concurrent.Future
import scala.util.Try

class RunningServer private (val settings: Settings, bindingFuture: Future[Seq[Http.ServerBinding]])

object RunningServer {

  def apply(settings: Settings) = {
    import settings.implicits._
    val httpsRoutes: Route = makeRoutes
    val https: HttpsConnectionContext = loadHttps(settings.sslConfig).get

    val httpsBindingFuture = {
      Http().bindAndHandle(httpsRoutes, settings.host, settings.port, connectionContext = https)
    }

    // TODO - check the schema in the route, not offer a different port ... perhaps
    val httpBindingFuture = {
      val httpRoutes: Route = EsaRoutes.http(StaticFileRoutes.dev.http())
      Http().bindAndHandle(httpRoutes, settings.host, settings.port + 1)
    }

    val bindingFuture: Future[Seq[Http.ServerBinding]] = Future.sequence(Seq(httpsBindingFuture, httpBindingFuture))

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
