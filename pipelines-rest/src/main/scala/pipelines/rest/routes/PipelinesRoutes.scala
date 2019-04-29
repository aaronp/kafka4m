package pipelines.rest.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import args4c.RichConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import org.apache.kafka.clients.consumer.ConsumerConfig.{CLIENT_ID_CONFIG, GROUP_ID_CONFIG}
import org.apache.kafka.clients.consumer.ConsumerRecord
import pipelines.admin.GenerateServerCertRequest
import pipelines.connect.{Bytes, KafkaFacade, RichKafkaConsumer, _}
import pipelines.eval.{AvroReader, EvalReactive}
import pipelines.kafka.{KafkaEndpoints, ListTopicsResponse, PullLatestResponse, QueryRequest}

class PipelinesRoutes(kafka: KafkaFacade, newStreamHandler: PipelinesRoutes.IsBinaryStream => Flow[Message, Message, NotUsed])
    extends KafkaEndpoints
    with BaseCirceRoutes
    with AutoCloseable {

  implicit def generateServerCertRequestSchema: JsonSchema[GenerateServerCertRequest] = JsonSchema(implicitly, implicitly)
  implicit def listTopicsResponseSchema: JsonSchema[ListTopicsResponse]               = JsonSchema(implicitly, implicitly)
  implicit def pullLatestResponseSchema: JsonSchema[PullLatestResponse]               = JsonSchema(implicitly, implicitly)

  def listTopicsRoute: Route = listTopics.listTopicsEndpoint.implementedBy { _ =>
    kafka.listTopics()
  }

  def pullLatestRoute: Route = query.pullEndpoint.implementedBy {
    case (topic, offset, limit) => kafka.pullLatest(topic, offset, limit)
  }

  val streamRoute: Route = {
    stream.streamEndpoint.request { isBinary =>
      val handler: Flow[Message, Message, NotUsed] = newStreamHandler(isBinary.getOrElse(false))
      handleWebSocketMessages(handler)
    }
  }

  def routes: Route = streamRoute ~ listTopicsRoute ~ pullLatestRoute

  override def close(): Unit = {
    kafka.close()
  }
}

object PipelinesRoutes extends StrictLogging {

  type IsBinaryStream = Boolean

  import args4c.implicits._
  def apply(rootConfig: Config)(implicit mat: ActorMaterializer, ioScheduler: Scheduler): PipelinesRoutes = forRoot(rootConfig)

  private def forRoot(rootConfig: Config)(implicit mat: ActorMaterializer, ioScheduler: Scheduler): PipelinesRoutes = {

    import args4c.implicits._

    val pollTimeout = rootConfig.asFiniteDuration("pipelines.consumer.pollTimeout")
    val timeout     = rootConfig.asFiniteDuration("pipelines.consumer.timeout")

    val facade: KafkaFacade = {
      val schemasByTopic = KafkaFacade.schemasByTopicForRootConfig(rootConfig)
      KafkaFacade(newConsumer(rootConfig), schemasByTopic, pollTimeout, timeout, _.close())
    }

    val readerForTopic = (facade.schemaForTopic _).andThen(_.map(AvroReader.generic))

    def newStream(isBinary: PipelinesRoutes.IsBinaryStream): Flow[Message, Message, NotUsed] = {
      if (isBinary) {
        newBinarySocket(rootConfig, readerForTopic)
      } else {
        newTextSocket(rootConfig, readerForTopic)
      }
    }
    new PipelinesRoutes(facade, newStream)
  }

  private def newBinarySocket(rootConfig: Config, readerForTopic: EvalReactive.ReaderLookup)(implicit mat: ActorMaterializer,
                                                                                             ioScheduler: Scheduler): Flow[Message, Message, NotUsed] = {
    val reactive: EvalReactive[ConsumerRecord[String, Bytes]] = EvalReactive(clientForRequest(rootConfig, _))
    val binaryData                                            = formatBinaryStream(readerForTopic, reactive.source)
    SocketAdapter.asBinaryFlow(binaryData.toReactivePublisher, reactive.update)
  }

  private def newTextSocket(rootConfig: Config, readerForTopic: EvalReactive.ReaderLookup)(implicit mat: ActorMaterializer,
                                                                                           ioScheduler: Scheduler): Flow[Message, Message, NotUsed] = {
    val reactive: EvalReactive[ConsumerRecord[String, Bytes]] = EvalReactive(clientForRequest(rootConfig, _))
    val jsonData                                              = formatStream(readerForTopic, reactive.source)
    val publisher                                             = LoggingPublisher(jsonData.toReactivePublisher, "SOCKET.")
    SocketAdapter.asTextFlow(publisher, reactive.update)
  }

  private def clientForRequest(baseConfig: RichConfig, request: QueryRequest)(implicit ioScheduler: Scheduler): RichKafkaConsumer[String, Bytes] = {
    logger.info(s"Creating a new kafka client for $request")
    val config = baseConfig
      .set(s"pipelines.consumer.${GROUP_ID_CONFIG}", request.groupId)
      .set(s"pipelines.consumer.${CLIENT_ID_CONFIG}", request.clientId)
    val consumer: RichKafkaConsumer[String, Bytes] = newConsumer(config)

    consumer.subscribe(Set(request.topic), request.fromOffset)

    consumer
  }

  private def newConsumer(config: Config)(implicit scheduler: Scheduler): RichKafkaConsumer[String, Bytes] = RichKafkaConsumer.byteArrayValues(config)

}
