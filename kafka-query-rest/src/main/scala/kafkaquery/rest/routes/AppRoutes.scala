package kafkaquery.rest.routes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object AppRoutes {

  def https(staticRoutes: StaticFileRoutes, kafka: KafkaRoutes, theRest : Route*): Route = {
    val base = kafka.routes ~ staticRoutes.route ~ DocumentationRoutes.route
    theRest.foldLeft(base)(_ ~ _)
  }

  /**
    * @param staticRoutes the static paths
    * @return routes required to configure a new server -- one which doesn't have any credentials, etc set up
    */
  def setupRoutes(staticRoutes: StaticFileRoutes): Route = {
    staticRoutes.route ~ AdminRoutes.routes
  }

}
