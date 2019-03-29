package esa.rest.routes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object EsaRoutes {

  def https(staticRoutes: StaticFileRoutes, cs: CounterRoutes = new CounterRoutes): Route = {
    staticRoutes.route ~ cs.routes ~ DocumentationRoutes.route
  }

  def setupRoutes(staticRoutes: StaticFileRoutes): Route = {
    staticRoutes.route
  }

}
