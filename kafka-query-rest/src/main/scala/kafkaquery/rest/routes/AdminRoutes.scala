package kafkaquery.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.admin.GenerateServerCertRequest
import kafkaquery.admin.AdminEndpoints

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
    logger.info(s"${request.certificate} for ${request.saveToPath}}")

    ???
  }

  val setSeedRoute = seed.seedEndpoint.implementedBy { request =>
    ???
  }

  def routes = generateCertRoute ~ updateCertRoute ~ setSeedRoute
}
