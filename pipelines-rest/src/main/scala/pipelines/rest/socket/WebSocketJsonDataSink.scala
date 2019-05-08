package pipelines.rest.socket

import java.util.UUID

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}
import pipelines.core._
import pipelines.data.DataSink

import scala.concurrent.Future

object WebSocketJsonDataSink {
  def apply(useBinary: Boolean, id: String = UUID.randomUUID.toString)(implicit scheduler: Scheduler): WebSocketJsonDataSink = {
    val (input, output) = Pipe.publishToOne[String].unicast
    new WebSocketJsonDataSink(id, useBinary, input, output)
  }
}

/**
  *
  * @param id
  * @param input
  * @param output the messages coming from whatever source - the ones we have to turn into messages
  * @param scheduler
  */
class WebSocketJsonDataSink private[socket] (val id: String, useBinary: Boolean, input: Observer[String], output: Observable[String])(implicit scheduler: Scheduler)
    extends DataSink[String]
    with StrictLogging {

  @volatile private var clientAck: Future[Ack] = Continue

  private def onClientCancel() = {
    clientAck = Stop
  }

  override val sink = new Observer[String] {
    override def onNext(elem: String): Future[Ack] = {
      val future = input.onNext(elem)
      if (future == Continue) {
        clientAck
      } else {
        future
      }
    }

    override def onError(ex: Throwable): Unit = {
      logger.error(s"Source feed triggered onError($ex)", ex)
      input.onError(ex)
    }

    override def onComplete(): Unit = {
      logger.warn(s"Source feed triggered onComplete()")
      input.onComplete()
    }
  }

  /** @return an akka flow representation of the data coming through this sink
    */
  def flow(): Flow[Message, Message, NotUsed] = {
    def clientSink(): Sink[Message, Future[Done]] = {
      Sink.foreach[Message] { fromClient: Message =>
        if (fromClient.isText) {
          val json = fromClient.asTextMessage.asScala.getStrictText
          import io.circe.parser._
          decode[StreamingRequest](json) match {
            case Left(err)                => logger.error(s"Couldn't parse message from the client: '$json' : ${err}")
            case Right(Heartbeat)         =>
            case Right(CancelFeedRequest) => onClientCancel()
          }
        } else {
          logger.warn("Discarding binary message from client")
        }
      }
    }

    val messages = if (useBinary) {
      output.map(TextMessage.apply)
    } else {
      output.map(json => BinaryMessage(ByteString(json.getBytes("UTF-8"))))
    }
    Flow.fromSinkAndSource(clientSink, Source.fromPublisher(messages.toReactivePublisher))
  }

}
