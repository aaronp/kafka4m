package kafkaquery.rest.routes

import java.util.concurrent.ScheduledExecutorService

import akka.{Done, NotUsed}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import kafkaquery.connect.{Bytes, RichKafkaConsumer}
import kafkaquery.eval.KafkaReactive
import kafkaquery.kafka.StreamRequest
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher

import scala.concurrent.Future

/**
  * Convert data streams into web socket messages
  */
object SocketAdapter extends StrictLogging {

  /**
    * Messages understood from the client
    */
  sealed trait WebSocketClientRequest

  object WebSocketClientRequest {
    implicit object Format extends Encoder[WebSocketClientRequest] with Decoder[WebSocketClientRequest] {
      override def apply(a: WebSocketClientRequest): Json = {
        ???
      }

      override def apply(c: HCursor): Result[WebSocketClientRequest] = {
        ???
      }
    }
  }
  final case object CancelFeed                      extends WebSocketClientRequest
  final case class UpdateFeed(query: StreamRequest) extends WebSocketClientRequest

  def asPublisher(clientForQuery: StreamRequest => RichKafkaConsumer[String, Bytes], queryOut: Observable[StreamRequest])(
      implicit scheduler: Scheduler): Publisher[ConsumerRecord[String, Bytes]] = {
    val limited: Observable[ConsumerRecord[String, Bytes]] = {
      queryOut
        .map(q => q -> clientForQuery(q))
        .bracket {
          case (query, client) =>
            val kafka = Observable.fromIterator(Task(client.pull()))
            KafkaReactive(kafka, query.messageLimitPerSecond, query.streamStrategy)
        } { tuple =>
          Task.evalOnce(tuple._2.close())
        }
    }
    limited.toReactivePublisher
  }

  def recordsAsFlow(clientForQuery: StreamRequest => RichKafkaConsumer[String, Bytes], initialQuery: StreamRequest)(
      implicit scheduler: Scheduler): Flow[Message, Message, NotUsed] = {

    val queryPipe: (Observer[StreamRequest], Observable[StreamRequest]) = Pipe.publishToOne[StreamRequest].unicast
    val (queryIn, queryOut)                                             = queryPipe
    queryIn.onNext(initialQuery)

    val akkaSource: Source[ConsumerRecord[String, Bytes], NotUsed] = Source.fromPublisher(asPublisher(clientForQuery, queryOut))

    val messages: Source[Message, NotUsed] = akkaSource.map { record =>
      val key = record.key()
      TextMessage(key)
    }

    val kitchen: Sink[Message, Future[Done]] = Sink.foreach[Message] { fromClient =>
      if (fromClient.isText) {
        val json = fromClient.asTextMessage.asScala.getStrictText
        import io.circe.parser._
        decode[WebSocketClientRequest](json) match {
          case Left(err) =>
            logger.error(s"Couldn't parse message from the client: '$json' : ${err}")
          case Right(CancelFeed) =>
            queryIn.onComplete()
          case Right(UpdateFeed(newQuery)) =>
            queryIn.onNext(newQuery)
        }
      }
    }

    Flow.fromSinkAndSource(kitchen, messages)
  }

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

}
