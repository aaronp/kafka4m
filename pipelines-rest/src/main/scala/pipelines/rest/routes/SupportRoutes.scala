package pipelines.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import pipelines.core.GenericMessageResult
import pipelines.kafka.{KafkaSupportEndpoints, PublishMessage}

class SupportRoutes(rootConfig: Config, publisher: PublishMessage => Unit) extends KafkaSupportEndpoints with BaseCirceRoutes {

  def routes = {
    configRoute ~ publishRoute
  }

  def configRoute: Route = {
    config.configEndpoint.implementedBy { pathOpt =>
      import args4c.implicits._
      val text = pathOpt.fold(rootConfig)(rootConfig.withOnlyPath).summary()
      GenericMessageResult(text)
    }
  }

  def publishRoute: Route = {
    implicit def requestSchema: JsonSchema[PublishMessage] = JsonSchema(implicitly, implicitly)
    publishSupport.publishEndpoint.implementedBy { rqst: PublishMessage =>
      publisher(rqst)
      GenericMessageResult("ok")
    }
  }
}
