package esa.rest

import akka.http.scaladsl.server._
import Directives._

object EsaRoutes {

  def apply(staticRoutes: StaticFileServer, cs: CounterServer = new CounterServer): Route = {
    staticRoutes.route ~ cs.routes ~ DocumentationServer.route
  }

}
