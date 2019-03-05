package esa.rest
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import esa.rest.routes.{EsaRoutes, StaticFileRoutes}
import esa.rest.ssl.{HttpsUtil, SslConfig}

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object RestServerMain {

  def main(args: Array[String]): Unit = {

    val port                                      = 8080
    implicit val system                           = ActorSystem("my-system")
    implicit val materializer: ActorMaterializer  = ActorMaterializer()
    implicit val executionContext                 = system.dispatcher
    implicit val routingSettings: RoutingSettings = RoutingSettings(system)

    val httpsRoutes: Route = makeRoutes

    val https: HttpsConnectionContext = {
      import eie.io._
      val cert      = "/Users/aaron/dev/playground/esa/esa-rest/src/main/resources/scripts/target/crt/Aarons-MacBook-Pro.local.p12".asPath
      val sslConfig = SslConfig(cert, "password", "all your base are belong to us")
      loadHttps(sslConfig).get
    }

    val httpsBindingFuture = {
      //Http().setDefaultClientHttpsContext(https)
      Http().bindAndHandle(httpsRoutes, "0.0.0.0", port, connectionContext = https)
    }

    // TODO - check the schema in the route, not offer a different port ... perhaps
    val httpBindingFuture = {
      val httpRoutes: Route = EsaRoutes.http(StaticFileRoutes.devHttp)
      Http().bindAndHandle(httpRoutes, "0.0.0.0", port + 1)
    }

    val bindingFuture = Future.sequence(Seq(httpsBindingFuture, httpBindingFuture))

    println(s"Server online at https://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap { both =>
        Future.sequence(both.map(_.unbind()))
      } // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  def makeRoutes(implicit routingSettings: RoutingSettings): Route = {
    val route = EsaRoutes.https(StaticFileRoutes.devHttps)
    Route.seal(route)
  }

  def loadHttps(sslConfig: SslConfig): Try[HttpsConnectionContext] = {
    sslConfig.withP12Certificate {
      case (pw, seed, is) => HttpsUtil(pw, seed, is, sslConfig.keystoreType)
    }
  }
}
