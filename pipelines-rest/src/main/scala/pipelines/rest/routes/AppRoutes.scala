package pipelines.rest.routes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object AppRoutes {

  def https(staticRoutes: StaticFileRoutes, kafka: PipelinesRoutes, theRest: Route*): Route = {
    val base = kafka.routes ~ staticRoutes.route ~ DocumentationRoutes.route
    theRest.foldLeft(base)(_ ~ _)
  }

}
