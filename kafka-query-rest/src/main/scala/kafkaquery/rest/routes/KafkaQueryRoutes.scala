package kafkaquery.rest.routes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object KafkaQueryRoutes {

  def https(staticRoutes: StaticFileRoutes): Route = {
    staticRoutes.route ~ DocumentationRoutes.route
  }

  def setupRoutes(staticRoutes: StaticFileRoutes): Route = {
    staticRoutes.route ~ AdminRoutes.routes
  }

}
