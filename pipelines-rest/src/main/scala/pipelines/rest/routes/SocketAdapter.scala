package pipelines.rest.routes

import akka.{Done, NotUsed}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging
import pipelines.connect.Bytes
import pipelines.kafka.StreamingFeedRequest
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.concurrent.Future

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

    def onMsg(fromClient: Message) =
      if (fromClient.isText) {
        val json = fromClient.asTextMessage.asScala.getStrictText
        import io.circe.parser._
        decode[StreamingFeedRequest](json) match {
          case Left(err)      => logger.error(s"Couldn't parse message from the client: '$json' : ${err}")
          case Right(request) => onRequest(request)
        }
      } else {
        logger.warn("Discarding binary message from client")
      }

//
//    Sink.fromSubscriber(new Subscriber[Message] with StrictLogging {
//      private var subscription: Subscription = null
//      private val batchSize                  = 16
//      @volatile private var pending          = 0
//      override def onSubscribe(s: Subscription): Unit = {
//        logger.info(s"SocketAdapter.onSubscribe($s)")
//        subscription = s
//        pending = batchSize / 2
//        subscription.request(batchSize)
//      }
//
//      override def onNext(t: Message): Unit = {
//        logger.info(s"SocketAdapter.onNext($t) w/ $pending pending")
//        onMsg(t)
//        pending = pending - 1
//        if (pending == 0) {
//          pending = batchSize / 2
//          subscription.request(batchSize)
//        }
//      }
//
//      override def onError(t: Throwable): Unit =
//        logger.info(s"SocketAdapter.onError($t) w/ $pending pending")
//
//      override def onComplete(): Unit =
//        logger.info(s"SocketAdapter.onComplete() w/ $pending pending")
//    })

    Sink.foreach[Message](onMsg)
  }
}
