package pipelines.rest.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import args4c.RichConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerConfig.{CLIENT_ID_CONFIG, GROUP_ID_CONFIG}
import org.apache.kafka.clients.consumer.ConsumerRecord
import pipelines.connect.{Bytes, KafkaFacade, RichKafkaConsumer}
import pipelines.eval.EvalReactive
import pipelines.kafka.{KafkaEndpoints, KafkaSchemas, QueryRequest, StreamingFeedRequest}

/** @param kafka a facade over kafka
  * @param createPublishingHandler the means to create a message flow for pushing our data to consumers and receiving [[StreamingFeedRequest]] messages
  * @param createConsumingHandler the means to create a message flow for taking data from publishers and sending back [[pipelines.kafka.StreamingRequest]] messages
  */
class KafkaRoutes(kafka: KafkaFacade,
                  createPublishingHandler: KafkaRoutes.IsBinaryStream => Flow[Message, Message, NotUsed],
                  createConsumingHandler: KafkaRoutes.IsBinaryStream => Flow[Message, Message, NotUsed])
    extends KafkaEndpoints
    with BaseCirceRoutes
    with KafkaSchemas
    with AutoCloseable {

  def listTopicsRoute: Route = listTopics.listTopicsEndpoint.implementedBy { _ =>
    kafka.listTopics()
  }

  def pullLatestRoute: Route = query.pullEndpoint.implementedBy {
    case (topic, offset, limit) => kafka.pullLatest(topic, offset, limit)
  }

  def publishingRoute: Route = {
    publish.streamEndpoint.request { isBinary =>
      val handler: Flow[Message, Message, NotUsed] = createPublishingHandler(isBinary.getOrElse(false))
      handleWebSocketMessages(handler)
    }
  }
  def consumingRoute: Route = {
    consume.streamEndpoint.request { isBinary =>
      val handler: Flow[Message, Message, NotUsed] = createConsumingHandler(isBinary.getOrElse(false))
      handleWebSocketMessages(handler)
    }
  }

  def routes: Route = publishingRoute ~ consumingRoute ~ listTopicsRoute ~ pullLatestRoute

  override def close(): Unit = {
    kafka.close()
  }
}

object KafkaRoutes extends StrictLogging {

  type IsBinaryStream = Boolean

  type RecordFormatter[A] = (ConsumerRecord[String, Bytes]) => Observable[A]
  import args4c.implicits._
  def apply(rootConfig: Config)(implicit mat: ActorMaterializer, ioScheduler: Scheduler): KafkaRoutes = forRoot(rootConfig)

  private def forRoot(rootConfig: Config)(implicit mat: ActorMaterializer, ioScheduler: Scheduler): KafkaRoutes = {

    import args4c.implicits._

    val pollTimeout = rootConfig.asFiniteDuration("pipelines.consumer.pollTimeout")
    val timeout     = rootConfig.asFiniteDuration("pipelines.consumer.timeout")

    val facade: KafkaFacade = {
      val schemasByTopic = KafkaFacade.schemasByTopicForRootConfig(rootConfig)
      KafkaFacade(newConsumer(rootConfig), schemasByTopic, pollTimeout, timeout, _.close())
    }

    def newPublishingStream(isBinary: KafkaRoutes.IsBinaryStream): Flow[Message, Message, NotUsed] = {
      val reactive: EvalReactive[ConsumerRecord[String, Bytes]]                         = EvalReactive(clientForRequest(rootConfig, _))
      val queriesWithRecords: Observable[(QueryRequest, ConsumerRecord[String, Bytes])] = reactive.source
      val obs: Observable[Message] = queriesWithRecords.flatMap {
        case (query, record) => SocketAdapter.asMessage(isBinary, query, record, facade.descriptorForTopic)
      }
      Flow.fromSinkAndSource(SocketAdapter.asSink(reactive.update), Source.fromPublisher(obs.toReactivePublisher))
    }

    def newConsumingStream(isBinary: KafkaRoutes.IsBinaryStream): Flow[Message, Message, NotUsed] = {
      val heartbeatFrequency = rootConfig.asFiniteDuration("pipelines.publisher.heartbeatFrequency")
      if (isBinary) {
        SocketAdapter.consuming.asFlow(heartbeatFrequency) { binaryData: ByteString =>
          //
          // TODO
          //
          ???
        }
      } else {
        SocketAdapter.consuming.asTextFlow(heartbeatFrequency) { textData: String =>
          //
          // TODO
          //
          ???
        }
      }
    }

    new KafkaRoutes(facade, newPublishingStream, newConsumingStream)
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
