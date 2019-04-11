package kafkaquery.rest.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import args4c.RichConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.connect.{Bytes, KafkaFacade, RichKafkaConsumer}
import kafkaquery.eval.KafkaReactive
import kafkaquery.kafka.{KafkaEndpoints, QueryRequest}
import monix.execution.Scheduler
import org.apache.kafka.clients.consumer.ConsumerConfig.{CLIENT_ID_CONFIG, GROUP_ID_CONFIG}
import org.apache.kafka.clients.consumer.ConsumerRecord
import concurrent.duration._

import scala.util.Try

class KafkaRoutes(kafka: KafkaFacade, newStreamHandler: () => Flow[Message, Message, NotUsed]) extends KafkaEndpoints with BaseRoutes with StrictLogging {

  val listTopicsRoute: Route = listTopics.listTopicsEndpoint.implementedBy { _ =>
    kafka.listTopics()
  }

  val pullLatestRoute: Route = pullLatest.pullEndpoint.implementedBy {
    case (topic, offset, limit) => kafka.pullLatest(topic, offset, limit)
  }

  val streamRoute: Route = {
    val se = stream.streamEndpoint
    se.request { _ =>
      val handler: Flow[Message, Message, NotUsed] = newStreamHandler()
      handleWebSocketMessages(handler)
    }
  }

  def routes: Route = streamRoute ~ listTopicsRoute ~ pullLatestRoute
}

object KafkaRoutes extends StrictLogging {
  import args4c.implicits._
  def apply(rootConfig: Config)(implicit mat: ActorMaterializer, scheduler: Scheduler): KafkaRoutes = forRoot(rootConfig)

  private def forRoot(rootConfig: Config)(implicit mat: ActorMaterializer, scheduler: Scheduler): KafkaRoutes = {

    import args4c.implicits._

    val pollTimeout = rootConfig.asFiniteDuration("kafkaquery.consumer.pollTimeout")
    val timeout     = rootConfig.asFiniteDuration("kafkaquery.consumer.timeout")

    val facade = KafkaFacade(newConsumer(rootConfig), pollTimeout, timeout)

    new KafkaRoutes(facade, () => newBinarySocket(rootConfig))
  }

  // TODO - send a socket with a json text body including a 'key' and a 'value' that has a Base64 encoded byte string
  private def newBinarySocket(rootConfig: Config)(implicit mat: ActorMaterializer, scheduler: Scheduler): Flow[Message, Message, NotUsed] = {
    // TODO - filter records on the expression
    def newFilter(expression: String): ConsumerRecord[String, Bytes] => Boolean = (_: ConsumerRecord[String, Bytes]) => true

    val reactive: KafkaReactive[ConsumerRecord[String, Bytes]] = KafkaReactive(newFilter, clientForRequest(rootConfig, _))
//    val bytes                                                  = reactive.source.dump("binary socket yields").map(_.value)
//    SocketAdapter.asBinaryFlow(bytes.toReactivePublisher, reactive.update)
    val keys = reactive.source.dump("text socket yields").map(_.key)
    SocketAdapter.asTextFlow(keys.toReactivePublisher, reactive.update)
  }

  def clientForRequest(baseConfig: RichConfig, request: QueryRequest): RichKafkaConsumer[String, Bytes] = {
    logger.info(s"Creating a new kafka client for $request")

    val config = baseConfig
      .set(s"kafkaquery.consumer.${GROUP_ID_CONFIG}", request.groupId)
      .set(s"kafkaquery.consumer.${CLIENT_ID_CONFIG}", request.clientId)
    val consumer: RichKafkaConsumer[String, Bytes] = newConsumer(config)

    consumer.subscribe(Set(request.topic), request.fromOffset)

    consumer
  }

  private def newConsumer(config: Config): RichKafkaConsumer[String, Bytes] = RichKafkaConsumer.byteArrayValues(config)

}
