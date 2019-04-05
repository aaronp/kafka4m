package kafkaquery.rest.routes

import java.util.concurrent.ScheduledExecutorService

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import args4c.RichConfig
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.connect.{Bytes, KafkaFacade, RichKafkaConsumer}
import kafkaquery.kafka.{KafkaEndpoints, ListTopicsResponse, StreamRequest}

class KafkaRoutes(kafka: KafkaFacade, newStreamHandler: StreamRequest => Flow[Message, Message, NotUsed]) extends KafkaEndpoints with BaseRoutes with StrictLogging {

  val listTopicsRoute: Route = listTopics.listTopicsEndpoint.implementedBy { _ =>
    kafka.listTopics()
  }

  val pullLatestRoute: Route = pullLatest.pullEndpoint.implementedBy {
    case (topic, offset, limit) => kafka.pullLatest(topic, offset, limit)
  }

  val streamRoute: Route = {
    stream.streamEndpoint.request { query: StreamRequest =>
      val handler = newStreamHandler(query)
      handleWebSocketMessages(handler)
    }
  }

  def routes: Route = streamRoute ~ listTopicsRoute ~ pullLatestRoute
}

object KafkaRoutes {
  import args4c.implicits._
  def apply(rootConfig: Config)(implicit mat: ActorMaterializer, scheduler: ScheduledExecutorService): KafkaRoutes = forRoot(rootConfig)

  def greeterWebSocketService(implicit mat: ActorMaterializer, scheduler: ScheduledExecutorService): Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .mapConcat {
        // we match but don't actually consume the text message here,
        // rather we simply stream it back as the tail of the response
        // this means we might start sending the response even before the
        // end of the incoming message has been received
        case tm: TextMessage   => TextMessage(Source.single("Hello ") ++ tm.textStream) :: Nil
        case bm: BinaryMessage =>
          // ignore binary messages but drain content to avoid the stream being clogged
          bm.dataStream.runWith(Sink.ignore)
          Nil
      }
  }

  private def forRoot(rootConfig: RichConfig)(implicit mat: ActorMaterializer, scheduler: ScheduledExecutorService): KafkaRoutes = {
    val consumerConfig                             = rootConfig.kafkaquery.consumer.config
    val consumer: RichKafkaConsumer[String, Bytes] = RichKafkaConsumer.byteArrayValues(rootConfig.config)
    val facade = KafkaFacade(
      kafka = consumer,
      pollTimeout = consumerConfig.asFiniteDuration("pollTimeout"),
      timeout = consumerConfig.asFiniteDuration("timeout")
    )
    new KafkaRoutes(facade, _ => greeterWebSocketService)
  }
}
