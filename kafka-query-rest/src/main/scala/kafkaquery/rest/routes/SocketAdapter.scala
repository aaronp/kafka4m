package kafkaquery.rest.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import kafkaquery.connect.Bytes
import kafkaquery.kafka.StreamingFeedRequest
import org.reactivestreams.Publisher

/** Functions to convert a data stream into an akka web socket flow
  */
object SocketAdapter extends StrictLogging {

  def asTextFlow(source: Publisher[String], onRequest: StreamingFeedRequest => Unit): Flow[Message, Message, NotUsed] = {
    val akkaSource: Source[String, NotUsed] = Source.fromPublisher(source)
    val messages: Source[Message, NotUsed]  = akkaSource.map(TextMessage.apply)
    Flow.fromSinkAndSource(asSink(onRequest), messages)
  }

  def asBinaryFlow(source: Publisher[Bytes], onRequest: StreamingFeedRequest => Unit): Flow[Message, Message, NotUsed] = {
    val akkaSource: Source[Bytes, NotUsed] = Source.fromPublisher(source)
    val messages                           = akkaSource.map(data => BinaryMessage(ByteString(data)))
    Flow.fromSinkAndSource(asSink(onRequest), messages)
  }

  def asSink[A](onRequest: StreamingFeedRequest => Unit) = {
    Sink.foreach[Message] { fromClient =>
      if (fromClient.isText) {
        val json = fromClient.asTextMessage.asScala.getStrictText
        import io.circe.parser._
        decode[StreamingFeedRequest](json) match {
          case Left(err)      => logger.error(s"Couldn't parse message from the client: '$json' : ${err}")
          case Right(request) => onRequest(request)
        }
      }
    }
  }
}
