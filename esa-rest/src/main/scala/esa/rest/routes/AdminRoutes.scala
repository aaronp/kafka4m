package esa.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esa.endpoints.{AdminEndpoints, GenerateServerCertRequest}

object AdminRoutes extends AdminEndpoints with BaseRoutes {

  val generateCertRoute: Route = generate.generateEndpoint.implementedBy {
    case GenerateServerCertRequest(saveToPath) => ???
  }

  val updateCertRoute = updatecert.updateEndpoint.implementedBy { request =>
    ???
  }

  val setSeedRoute = seed.seedEndpoint.implementedBy { request =>
    ???
  }

  def routes = generateCertRoute ~ updateCertRoute ~ setSeedRoute
}
