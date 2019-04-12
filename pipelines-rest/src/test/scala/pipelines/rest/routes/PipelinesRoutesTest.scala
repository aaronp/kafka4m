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
class PipelinesRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with FailFastCirceSupport with ScalaFutures {

  object testService extends KafkaFacade {
    val cannedResponse = ListTopicsResponse(Map("topic1" -> List(PartitionData(1, "of the pack")), "topic2" -> List(PartitionData(2, "nope"))))
    override def listTopics() = {
      cannedResponse
    }

    override def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse = {
      PullLatestResponse(topic, List("fake-key"))
    }

    override def schemaForTopic(topic: String): Option[String] = None
  }

  "GET /kafka/topics" should {
    "list the topics" in {
      val routes = new PipelinesRoutes(testService, _ => ???).routes

      Get("/kafka/topics") ~> routes ~> check {
        response.status.isSuccess() shouldBe true
        entityAs[ListTopicsResponse] shouldBe testService.cannedResponse
      }
    }
  }

  "GET /kafka/stream" should {
    "open a web socket to pull all data from a query" in {
      val wsClient = WSProbe()

      val expectedData     = (0 to 10).map(i => s"data $i")
      val clientRequest    = UpdateFeedRequest(QueryRequestTest.expected)
      val receivedRequests = ListBuffer[StreamingFeedRequest]()
      val websocketRoute: Route = {
        implicit def sched = Scheduler.global

        def onUpdate(request: StreamingFeedRequest) = {
          receivedRequests += request
        }

        val flow: Flow[Message, Message, NotUsed] = {
          val data = Observable.fromIterable(expectedData)
          SocketAdapter.asTextFlow(data.toReactivePublisher, onUpdate)
        }
        new PipelinesRoutes(testService, _ => flow).routes
      }

      WS("/kafka/stream", wsClient.flow) ~> websocketRoute ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        // manually run a WS conversation
        import io.circe.syntax._
        val fromClient = clientRequest.asJson

        wsClient.sendMessage(fromClient.noSpaces)
        expectedData.foreach { expected =>
          wsClient.expectMessage(expected)
        }

        receivedRequests should contain(clientRequest)

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
    }

  }
}
