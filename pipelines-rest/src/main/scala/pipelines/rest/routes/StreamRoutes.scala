package pipelines.rest.routes

import akka.http.scaladsl.server.{Directive, Route}

import akka.http.scaladsl.server.Directives._
import pipelines.data.DataRegistry
import pipelines.stream.{ListSourceResponse, StreamEndpoints, StreamSchemas}

class StreamRoutes(registry: DataRegistry) extends StreamEndpoints with StreamSchemas with BaseCirceRoutes with AutoCloseable {

  def listSourcesRoute: Route = {
    val x: JsonResponse[ListSourceResponse] = implicitly[JsonResponse[ListSourceResponse]]
    list.listSourcesEndpoint(x).implementedBy { _ =>
      registry.sources.list()
    }
  }
  def websocketConsumeRoute: Route = {
    ???
  }
  def peekRoute: Route = {
    ???
  }
  def copyRoute: Route = {
    ???
  }
  def updateRoute: Route = {
    ???
  }
  def createRoute: Route = {
    ???
  }
  def pushRoute: Route = {
    ???
  }
  def websocketPublishRoute: Route = {
    ???
  }

  def routes: Route = {
    listSourcesRoute ~ websocketConsumeRoute ~ peekRoute ~ copyRoute ~ updateRoute ~ createRoute ~ pushRoute ~ websocketPublishRoute
  }

  override def close(): Unit = {}
}
