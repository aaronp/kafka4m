package pipelines.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.circe.Json
import monix.execution.Ack.Continue
import pipelines.core.CreateSourceRequest
import pipelines.data._
import pipelines.rest.socket.{SourceFactory, WebSocketJsonDataSink}
import pipelines.stream.{ListSourceResponse, PeekResponse, StreamEndpoints, StreamSchemas}

import scala.concurrent.Future

class StreamRoutes(registry: DataRegistry, sourceFactory: SourceFactory)(implicit adapterEvidence: TypeAdapter.Aux, filter: FilterAdapter, persistDir: PersistLocation)
    extends StreamEndpoints
    with StreamSchemas
    with BaseCirceRoutes
    with AutoCloseable {

  def listSourcesRoute: Route = {
    val x: JsonResponse[ListSourceResponse] = implicitly[JsonResponse[ListSourceResponse]]
    list.listSourcesEndpoint(x).implementedBy { _ =>
      registry.sources.list()
    }
  }

  // create and consume from a registered source (assuming that source produces json) in one step
  def websocketConsumeRoute: Route = {
    websocketConsume.consumeEndpoint.implementedBy {
      case (sourceId, isBinaryOpt) =>
        implicit val sched = registry.defaultIOScheduler
        val newConsumer    = WebSocketJsonDataSink(isBinaryOpt.getOrElse(false))

        val sinkRegistered = registry.sinks.register(newConsumer.id, newConsumer)
        if (!sinkRegistered) {
          throw new Exception(s"Error registering sink ${newConsumer.id}")
        }

        registry.connect(sourceId, newConsumer.id) match {
          case ConnectResponse(_, _) => handleWebSocketMessages(newConsumer.flow())
          case result                => throw new Exception(s"Error connecting new sink ${newConsumer.id}: $result")
        }
    }
  }

  // create a new source with data from a websocket. If a source already exists for the specified ID, then we'll try and push to it
  def websocketPublishRoute: Route = {
    websocketPublish.publishEndpoint.implementedBy {
      case (Some(sourceId), isBinaryOpt) =>
        registry.sources.get(sourceId) match {
          case Some(push : DataSource.PushSource[Json]) =>
          case Some(other) =>
          case None =>
        }
      case (None, isBinaryOpt) =>
    }
  }

  def peekRoute: Route = {
    peek.peekEndpoint.implementedByAsync { sourceId =>
      registry.sources.get(sourceId) match {
        case None =>
          Future.successful(PeekResponse(Json.Null))
        case Some(source) =>
          implicit val ec = registry.defaultIOScheduler
          source.data.firstOptionL.runToFuture.map {
            case None                    => PeekResponse(Json.Null)
            case Some(head: Json)        => PeekResponse(head)
            case Some(head: String)      => PeekResponse(Json.fromString(head))
            case Some(head: Array[Byte]) => PeekResponse(Json.fromString(new String(head, "UTF-8")))
            case Some(other)             => PeekResponse(Json.fromString(other.toString))
          }
      }
    }
  }

  def copyRoute: Route = {
    copy.copyEndpoint.implementedBy {
      case (sourceId, enrichment) => registry.update(EnrichSourceRequest(sourceId, s"${sourceId}.${enrichment}", enrichment))
    }
  }

  def updateRoute: Route = {
    update.updateEndpoint.implementedBy {
      case (sourceId, enrichment) => registry.update(UpdateEnrichedSourceRequest(sourceId, enrichment))
    }
  }

  def createRoute: Route = {
    create.createEndpoint.implementedBy {
      case (sourceIdOpt, createSource: CreateSourceRequest) => sourceFactory.create(createSource, sourceIdOpt)
    }
  }

  def pushRoute: Route = {
    push.pushEndpoint.implementedByAsync {
      case (id, data) =>
        registry.sources.get(id) match {
          case Some(push: DataSource.PushSource[Json]) =>
            implicit val ec = registry.defaultIOScheduler
            push.push(data).map(_ == Continue)
          case Some(other) =>
            val err = new Exception(s"Source ${id} was not a push source, but rather ${other}")
            logger.error(err.getMessage, err)
            Future.failed(err)
          case None =>
            val err = new Exception(s"Source ${id} not found")
            logger.error(err.getMessage, err)
            Future.failed(err)
        }
    }
  }

  def routes: Route = {
    listSourcesRoute ~ websocketConsumeRoute ~ peekRoute ~ copyRoute ~ updateRoute ~ createRoute ~ pushRoute ~ websocketPublishRoute
  }

  override def close(): Unit = {
    registry.close()
  }
}
