package kafkaquery.rest

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.rest.routes.{AppRoutes, KafkaRoutes, StaticFileRoutes}
import kafkaquery.rest.ssl.{HttpsUtil, SslConfig}

import scala.concurrent.Future
import scala.util.Try

class RunningServer private (val settings: Settings, bindingFuture: Future[Http.ServerBinding])

object RunningServer extends StrictLogging {

  def apply(settings: Settings, sslConf: SslConfig) = {
    import settings.implicits._
    val httpsRoutes: Route            = makeRoutes(settings.staticRoutes, settings.kafkaRoutes, settings.kafkaSupportRoutes.routes)
    val https: HttpsConnectionContext = loadHttps(sslConf).get

    logger.info(s"Starting with ${settings}")

    val flow: Flow[HttpRequest, HttpResponse, NotUsed] = route2HandlerFlow(httpsRoutes)
    val httpsBindingFuture                             = Http().bindAndHandle(flow, settings.host, settings.port, connectionContext = https)
    val bindingFuture                                  = httpsBindingFuture
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
