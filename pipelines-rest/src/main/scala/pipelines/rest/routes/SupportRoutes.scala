package pipelines.rest.routes

import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import pipelines.core.GenericMessageResult
import pipelines.kafka.{KafkaSupportEndpoints, PublishMessage}
import akka.http.scaladsl.server.Directives._

class SupportRoutes(rootConfig: Config, publisher: PublishMessage => Unit) extends KafkaSupportEndpoints with BaseRoutes with StrictLogging {

  def routes = {
    configRoute ~ publishRoute
  }

  val configRoute: Route = config.configEndpoint.implementedBy { pathOpt =>
    import args4c.implicits._
    val text = pathOpt.fold(rootConfig)(rootConfig.withOnlyPath).summary()
    GenericMessageResult(text)
  }

  val publishRoute: Route = publish.publishEndpoint.implementedBy { rqst: PublishMessage =>
    publisher(rqst)
    GenericMessageResult("ok")
  }
}
