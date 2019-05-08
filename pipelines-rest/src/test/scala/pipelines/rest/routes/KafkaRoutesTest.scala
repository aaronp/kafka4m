package pipelines.rest.routes

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import pipelines.connect.KafkaFacade
import pipelines.kafka._
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import concurrent.duration._

import scala.collection.mutable.ListBuffer

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
class KafkaRoutesTest extends BaseRoutesTest {

  object testService extends KafkaFacade {
    val cannedResponse = ListTopicsResponse(Map("topic1" -> List(PartitionData(1, "of the pack")), "topic2" -> List(PartitionData(2, "nope"))))
    override def listTopics() = {
      cannedResponse
    }

    override def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse = {
      PullLatestResponse(topic, List("fake-key"))
    }

    override def descriptorForTopic(topic: String) = None

    override def close(): Unit = {}
  }

  "GET /kafka/topics" should {
    "list the topics" in {
      val routes = new KafkaRoutes(testService, _ => ???, _ => ???).routes

      Get("/kafka/topics") ~> routes ~> check {
        response.status.isSuccess() shouldBe true
        entityAs[ListTopicsResponse] shouldBe testService.cannedResponse
      }
    }
  }
//
//  "GET /stream/pull" should {
//    "open a web socket to pull all data from a query" in {
//      val wsClient = WSProbe()
//
//      val expectedData     = (0 to 10).map(i => s"data $i")
//      val clientRequest    = UpdateFeedRequest(QueryRequestTest.expected)
//      val receivedRequests = ListBuffer[StreamingFeedRequest]()
//      val websocketRoute: Route = {
//        implicit def sched = Scheduler.global
//
//        def onUpdate(request: StreamingFeedRequest) = {
//          receivedRequests += request
//        }
//
//        val flow: Flow[Message, Message, NotUsed] = {
//          val data = Observable.fromIterable(expectedData)
//          SocketAdapter.consuming.asTextFlow(data.toReactivePublisher, onUpdate)
//        }
//        new KafkaRoutes(testService, _ => flow, _ => ???).routes
//      }
//
//      WS("/stream/pull", wsClient.flow) ~> websocketRoute ~> check {
//        // check response for WS Upgrade headers
//        isWebSocketUpgrade shouldEqual true
//
//        // manually run a WS conversation
//        import io.circe.syntax._
//        val fromClient = clientRequest.asJson
//
//        wsClient.sendMessage(fromClient.noSpaces)
//        expectedData.foreach { expected =>
//          wsClient.expectMessage(expected)
//        }
//
//        receivedRequests should contain(clientRequest)
//
//        wsClient.sendCompletion()
//        wsClient.expectCompletion()
//      }
//    }
//  }
//  "GET /stream/push" should {
//    "open a web socket to push data" in {
//      val wsClient = WSProbe()
//
////      val expectedData     = (0 to 10).map(i => s"data $i")
////      val clientRequest    = UpdateFeedRequest(QueryRequestTest.expected)
//      val receivedData = ListBuffer[String]()
//      val websocketRoute: Route = {
//        implicit def sched = Scheduler.global
//        val flow: Flow[Message, Message, NotUsed] = {
//          SocketAdapter.producer.asTextFlow(1.second) { text =>
//            receivedData += text
//            receivedData.size < 10
//          }
//        }
//        new KafkaRoutes(testService, _ => flow).routes
//      }
//
//      WS("/stream/push", wsClient.flow) ~> websocketRoute ~> check {
//        // check response for WS Upgrade headers
//        isWebSocketUpgrade shouldEqual true
//
//        // manually run a WS conversation
//        import io.circe.syntax._
//        val fromClient = clientRequest.asJson
//
//        wsClient.sendMessage(fromClient.noSpaces)
//
//        expectedData.foreach { expected =>
//          wsClient.expectMessage(expected)
//        }
//
//        receivedRequests should contain(clientRequest)
//
//        wsClient.sendCompletion()
//        wsClient.expectCompletion()
//      }
//    }
//  }
}
