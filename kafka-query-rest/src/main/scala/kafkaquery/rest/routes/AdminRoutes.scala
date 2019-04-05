package kafkaquery.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.endpoints.{AdminEndpoints, GenerateServerCertRequest}

import scala.util.control.NonFatal

object AdminRoutes extends AdminEndpoints with BaseRoutes with StrictLogging {

  val generateCertRoute: Route = generate.generateEndpoint.implementedBy {
    case GenerateServerCertRequest(saveToPath) =>
      try {

        ???
      } catch {
        case NonFatal(e) =>
          ???
      }
  }

  val updateCertRoute = updatecert.updateEndpoint.implementedBy { request =>
  request.certificate
  request.saveToPath

    ???
  }

  val setSeedRoute = seed.seedEndpoint.implementedBy { request =>
    ???
  }

  def routes = generateCertRoute ~ updateCertRoute ~ setSeedRoute
}
