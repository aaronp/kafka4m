package pipelines.rest.routes

import akka.actor.Cancellable
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.StrictLogging
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord
import pipelines.DynamicAvroRecord
import pipelines.connect._
import pipelines.eval.AvroReader
import pipelines.kafka.{CancelFeedRequest, Heartbeat, QueryRequest, StreamingFeedRequest, StreamingRequest}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import io.circe.syntax._

/** Functions to convert a data stream into an akka web socket flow
  */
object SocketAdapter extends StrictLogging {

  def asMessage(isBinary: Boolean, query: QueryRequest, record: ConsumerRecord[String, Bytes], descriptorForTopic: String => Option[TopicDescriptor]): Observable[Message] = {
    descriptorForTopic(query.topic) match {
      case Some(AvroDescriptor(schema)) =>
        val reader                             = AvroReader.generic(schema)
        val avroResult: Try[DynamicAvroRecord] = reader.read(record.value)
        if (isBinary) {
          avroRecordResultAsBinary(record, avroResult, query).map(bytes => BinaryMessage(ByteString(bytes)))
        } else {
          avroRecordResultAsRecordJson(record, avroResult, query).map(jsonRecord => TextMessage(jsonRecord.asJson.noSpaces))
        }
      case Some(JsonDescriptor) =>
        Observable(RecordJson(record, record.key.toString, s"${record.value.length} bytes")).map(jsonRecord => TextMessage(jsonRecord.asJson.noSpaces))
      case Some(ProtobufDescriptor(protobuffSchema)) =>
        //
        // TODO
        //
        ???
      case None =>
        val x = RecordJson(record, record.key.toString, new String(record.value, "UTF-8"))
        Observable(TextMessage(x.asJson.noSpaces))
    }
  }

  // the websocket feeds coming into the system
  object consuming {
    type Continue = Boolean

    private class Wrapper(var msg: StreamingRequest) {
      def asTextMessage: TextMessage.Strict = {
        val json = StreamingRequest.encodeRequest(msg).noSpaces
        TextMessage(json)
      }
    }

    def asTextFlow(heartbeatFrequency: FiniteDuration)(onNext: String => Continue): Flow[Message, Message, NotUsed] = {
      asFlow(heartbeatFrequency) { bytes =>
        onNext(bytes.utf8String)
      }
    }

    def asFlow(heartbeatFrequency: FiniteDuration)(onNext: ByteString => Continue): Flow[Message, Message, NotUsed] = {
      val wrapper                                  = new Wrapper(Heartbeat)
      val akkaSource: Source[Wrapper, Cancellable] = Source.tick(heartbeatFrequency, heartbeatFrequency, wrapper)
      val sink = Sink.foreach[Message] { fromClient =>
        val bytes = if (fromClient.isText) {
          ByteString(fromClient.asTextMessage.getStrictText.getBytes("UTF-8"))
        } else {
          fromClient.asBinaryMessage.getStrictData
        }
        val continue = onNext(bytes)
        if (!continue) {
          logger.info("Cancelling incoming feed")
          wrapper.msg = CancelFeedRequest
        }
      }
      Flow.fromSinkAndSource(sink, akkaSource.map(_.asTextMessage))
    }
  }

  def asSink[A](onRequest: StreamingFeedRequest => Unit): Sink[Message, Future[Done]] = {
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

    Sink.foreach[Message](onMsg)
  }
}
