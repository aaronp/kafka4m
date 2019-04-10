package kafkaquery.rest

import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.rest.routes.{AppRoutes, KafkaRoutes, KafkaSupportRoutes, StaticFileRoutes}
import kafkaquery.rest.ssl.{HttpsUtil, SslConfig}

import scala.concurrent.Future
import scala.util.Try

class RunningServer private (val settings: Settings, bindingFuture: Future[Http.ServerBinding])

object RunningServer extends StrictLogging {

  def setup(settings: Settings): RunningServer = {

    logger.info(s"Configuration requires setup, running with ${settings}")
    import settings.implicits._
    // TODO - check the schema in the route, not offer a different port ... perhaps
    val httpBindingFuture = {
      val static            = StaticFileRoutes.fromRootConfig(settings.rootConfig).copy(landingPage = "setup.html")
      val httpRoutes: Route = AppRoutes.setupRoutes(static)
      Http().bindAndHandle(httpRoutes, settings.host, settings.port)
    }
    new RunningServer(settings, httpBindingFuture)
  }

  def apply(settings: Settings, sslConf: SslConfig) = {
    import settings.implicits._
    val httpsRoutes: Route            = makeRoutes(settings.staticRoutes, settings.kafkaRoutes, settings.kafkaSupportRoutes.routes)
    val https: HttpsConnectionContext = loadHttps(sslConf).get

    logger.info(s"Starting with ${settings}")

    val httpsBindingFuture = Http().bindAndHandle(httpsRoutes, settings.host, settings.port, connectionContext = https)
    val bindingFuture      = httpsBindingFuture
    new RunningServer(settings, bindingFuture)
  }

  def makeRoutes(static: StaticFileRoutes, kafka: KafkaRoutes, theRest: Route*)(implicit routingSettings: RoutingSettings): Route = {
    val route = AppRoutes.https(static, kafka, theRest: _*)
    Route.seal(route)
  }

  def loadHttps(sslConfig: SslConfig): Try[HttpsConnectionContext] = {
    sslConfig.withP12Certificate {
      case (pw, seed, is) => HttpsUtil(pw, seed, is, sslConfig.keystoreType)
    }
  }
}
