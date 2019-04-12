package pipelines.rest.routes

import java.util.UUID

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import io.circe.generic.auto._
import monix.execution.Scheduler
import org.scalatest.Matchers
import pipelines.connect.{BaseDockerSpec, RichKafkaProducer}
import pipelines.kafka._

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
class PipelinesRoutesIntegrationTest extends BaseDockerSpec("scripts/kafka") with Matchers with ScalatestRouteTest { //

  "GET /kafka/stream" should {
    "open a web socket to pull all data from a query" in {
      val wsClient = WSProbe()

      import args4c.implicits._

      val rootConfig = Array("dev.conf").asConfig()

      val expectedData = (0 to 10).map(i => s"data $i")

      val topic = s"testTopic${UUID.randomUUID}"

      val schedulerService = Scheduler.io("PipelinesRoutesIntegrationTest")
      try {
        val publisher: RichKafkaProducer[String, String] = RichKafkaProducer.strings(rootConfig)(schedulerService)

        expectedData.foreach { msg =>
          publisher.send(topic, msg, msg)
        }
        publisher.flush()

        val clientRequest = UpdateFeedRequest(
          new QueryRequest(
            clientId = "clientId" + UUID.randomUUID,
            groupId = "groupId" + UUID.randomUUID,
            topic = topic,
            filterExpression = "",
            filterExpressionIncludeMatches = true,
            fromOffset = Option("earliest"),
            messageLimit = None, //Option(Rate.perSecond(expectedData.size * 2)),
            format = None,
            streamStrategy = StreamStrategy.All
          ))

        val websocketRoute: Route = PipelinesRoutes(rootConfig)(materializer, schedulerService).routes

        val req: HttpRequest = WS("/kafka/stream", wsClient.flow)

        req ~> websocketRoute ~> check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true

          // manually run a WS conversation
          import io.circe.syntax._
          val fromClient = clientRequest.asJson

          println(expectedData.indices.size)
          println()
          wsClient.sendMessage(fromClient.noSpaces)
          val received = (0 until expectedData.size).map { _ =>
            wsClient.expectMessage()
          }
          received.size shouldBe expectedData.size

          val textReceived = received.map(_.asTextMessage.getStrictText)
          textReceived.foreach { json =>
            println(json)
            println()
          }

          wsClient.sendCompletion()
//          wsClient.expectCompletion()
        }
      } finally {
//        schedulerService.shutdown()
      }
    }
//    "open a web socket to rate limit data from a query" ignore {
//      val wsClient = WSProbe()
//
//      import args4c.implicits._
//
//      val rootConfig = Array("dev.conf").asConfig()
//
//      println()
//      println(rootConfig.getConfig("pipelines").summary())
//      println()
//
//      val expectedData = (0 to 10).map(i => s"data $i")
//
//      val topic = s"testTopic${UUID.randomUUID}"
//
//      implicit val schedulerService = Scheduler.io("PipelinesRoutesIntegrationTest2")
//      try {
//        val publisher = RichKafkaProducer.strings(rootConfig)
//
//        expectedData.foreach { msg =>
//          publisher.send(topic, msg, msg)
//        }
//        publisher.flush()
//
//        val clientRequest = UpdateFeedRequest(
//          new QueryRequest(
//            clientId = "clientId" + UUID.randomUUID,
//            groupId = "groupId" + UUID.randomUUID,
//            topic = topic,
//            filterExpression = "",
//            filterExpressionIncludeMatches = true,
//            fromOffset = Option("earliest"),
//            messageLimit = None, //Option(Rate.perSecond(expectedData.size * 2)),
//            format = None,
//            streamStrategy = StreamStrategy.All
//          ))
//
//        val receivedRequests = ListBuffer[StreamingFeedRequest]()
//        val websocketRoute: Route = PipelinesRoutes(rootConfig).routes
//
//        WS("/kafka/stream", wsClient.flow) ~> websocketRoute ~> check {
//          // check response for WS Upgrade headers
//          isWebSocketUpgrade shouldEqual true
//
//          // manually run a WS conversation
//          import io.circe.syntax._
//          val fromClient = clientRequest.asJson
//
//          println(expectedData.indices.size)
//          println()
//          wsClient.sendMessage(fromClient.noSpaces)
//          val received = (0 until expectedData.size).map { _ =>
//            wsClient.expectMessage()
//          }
//          received.size shouldBe expectedData.size
//
//          val textReceived = received.map(_.asTextMessage.getStrictText)
//          textReceived.foreach { json =>
//            println(json)
//            println()
//          }
//
//          wsClient.sendCompletion()
//          wsClient.expectCompletion()
//        }
//      } finally {
//        schedulerService.shutdown()
//      }
//    }

  }
}
