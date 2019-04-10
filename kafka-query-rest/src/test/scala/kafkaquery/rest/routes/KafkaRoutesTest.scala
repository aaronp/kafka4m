package kafkaquery.rest.routes

import akka.NotUsed
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model.{HttpMethods, RequestEntity}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import kafkaquery.connect.{Bytes, KafkaFacade}
import kafkaquery.kafka.{ListTopicsResponse, PartitionData, PullLatestResponse, StreamRequest, StreamStrategy}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
  * See
  *
  * https://github.com/akka/akka-http/blob/master/docs/src/test/scala/docs/http/scaladsl/server/WebSocketExampleSpec.scala
  *
  * and
  *
  * https://doc.akka.io/docs/akka-http/current/server-side/websocket-support.html
  *
  */
class KafkaRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with FailFastCirceSupport with ScalaFutures {

  "GET /kafka/topics" should {
    object service extends KafkaFacade {
      val cannedResponse = ListTopicsResponse(Map("topic1" -> List(PartitionData(1, "of the pack")), "topic2" -> List(PartitionData(2, "nope"))))
      override def listTopics() = {
        cannedResponse
      }

      override def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse = {
        PullLatestResponse(topic, List("fake-key"))
      }

      override def stream(request: StreamRequest): Publisher[ConsumerRecord[String, Bytes]] = ???
    }

    "list the topics" in {
      def onWebSocketRequest(req: StreamRequest) = ???
      val routes                                 = new KafkaRoutes(service, onWebSocketRequest).routes

      Get("/kafka/topics") ~> routes ~> check {
        response.status.isSuccess() shouldBe true
        entityAs[ListTopicsResponse] shouldBe service.cannedResponse
      }
    }
  }

  def greeterWebSocketService: Flow[Message, Message, NotUsed] = {
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

  def stream(implicit sys: ActorMaterializer): Route = {
    path("greeter") {
      get {
        val flow: Flow[Message, Message, NotUsed] = greeterWebSocketService

        val r: Route = handleWebSocketMessages(flow)
        r
      }
    }
  }

  def postStream(implicit sys: ActorMaterializer): Route = {
    post {
      path("posting") {
        entity(as[StreamRequest]) { rqt: StreamRequest =>
          val flow: Flow[Message, Message, NotUsed] = greeterWebSocketService
          handleWebSocketMessages(flow)
        }
      }
    }
  }

  "a websocket" should {
    "work w/ posts" in {
      val wsClient       = WSProbe()
      val websocketRoute = postStream

      val body = StreamRequest("client", "group", "topic", "", None, None, StreamStrategy.Latest)

      val request = WS("/posting", wsClient.flow).withMethod(HttpMethods.POST).withEntity(Marshal(body).to[RequestEntity].futureValue)

      request ~> websocketRoute ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        // manually run a WS conversation
        wsClient.sendMessage("Peter")
        wsClient.expectMessage("Hello Peter")

        wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
        wsClient.expectNoMessage(100.millis)

        wsClient.sendMessage("John")
        wsClient.expectMessage("Hello John")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
    }
    "work w/ gets" in {
      val wsClient       = WSProbe()
      val websocketRoute = stream

      WS("/greeter", wsClient.flow) ~> websocketRoute ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        // manually run a WS conversation
        wsClient.sendMessage("Peter")
        wsClient.expectMessage("Hello Peter")

        wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
        wsClient.expectNoMessage(100.millis)

        wsClient.sendMessage("John")
        wsClient.expectMessage("Hello John")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
    }

  }
}
