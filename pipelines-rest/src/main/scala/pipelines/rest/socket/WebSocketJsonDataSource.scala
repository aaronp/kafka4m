package pipelines.rest.socket

import java.util.UUID

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import pipelines.core.{DataType, JsonRecord}
import pipelines.data.DataSource

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object WebSocketJsonDataSource {
  def apply(heartbeatFrequency: FiniteDuration, id: String = UUID.randomUUID.toString)(implicit scheduler: Scheduler): WebSocketJsonDataSource = {

    val (input, output) = Pipe.publish[Message].multicast
    new WebSocketJsonDataSource(id, heartbeatFrequency, input, output)
  }

  private def msgAsText(msg: Message): String = {
    if (msg.isText) {
      msg.asTextMessage.getStrictText
    } else {
      msg.asBinaryMessage.getStrictData.utf8String
    }
  }

  /** @param source the push source
    * @param scheduler
    * @return a flow which will push data to this source and in turn send heartbeat messages back
    */
  def asFlow(heartbeatFrequency: FiniteDuration, source: DataSource.PushSource[String])(implicit scheduler: Scheduler): Flow[Message, Message, NotUsed] = {

    val toClient: Observable[TextMessage.Strict] = Observable.interval(heartbeatFrequency).map(_ => heartBeatTextMsg)
    val clientSink = Sink.foreach[Message] { msg =>
      source.push(msgAsText(msg))
    }
    Flow.fromSinkAndSource(clientSink, Source.fromPublisher(toClient.toReactivePublisher))
  }
}

/**
  * A data source which accepts input from a web socket
  */
class WebSocketJsonDataSource(id: String, heartbeatFrequency: FiniteDuration, input: Observer[Message], output: Observable[Message])(implicit scheduler: Scheduler)
    extends DataSource[String]
    with StrictLogging {
  override def tag: ClassTag[String] = {
    ClassTag[String](classOf[String])
  }

  override def sourceType: DataType = JsonRecord

  override def data: Observable[String] = output.map(WebSocketJsonDataSource.msgAsText)

  /** @return an akka flow representation of the data coming through this sink
    */
  def flow(): Flow[Message, Message, NotUsed] = {
    val toClient   = Observable.interval(heartbeatFrequency).map(_ => heartBeatTextMsg)
    val clientSink = Sink.fromSubscriber(input.toReactive)
    Flow.fromSinkAndSource(clientSink, Source.fromPublisher(toClient.toReactivePublisher))
  }
}
