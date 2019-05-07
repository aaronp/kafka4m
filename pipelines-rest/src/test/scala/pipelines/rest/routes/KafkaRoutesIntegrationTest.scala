package pipelines.rest.routes

import java.util.UUID

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import io.circe.parser._
import monix.execution.Scheduler
import org.scalatest.Matchers
import pipelines.Using
import pipelines.connect.{BaseDockerSpec, RecordJson, RichKafkaProducer}
import pipelines.core.StreamStrategy
import pipelines.kafka._

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
class KafkaRoutesIntegrationTest extends BaseDockerSpec("scripts/kafka") with Matchers with ScalatestRouteTest {

  "GET /kafka/pull" should {
    "open a web socket to pull all data from a query" in {
      val wsClient = WSProbe()

      import args4c.implicits._

      val rootConfig = Array("dev.conf").asConfig()

      val expectedData = (0 to 10).map(i => s"data $i")

      val topic = s"testTopic${UUID.randomUUID}"

      val schedulerService = Scheduler.io("PipelinesRoutesIntegrationTest")
      Using(RichKafkaProducer.strings(rootConfig)(schedulerService)) { publisher: RichKafkaProducer[String, String] =>
        try {
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

          Using(KafkaRoutes(rootConfig)(materializer, schedulerService)) { kafkaRoutes =>
            val websocketRoute: Route = kafkaRoutes.routes

            val req: HttpRequest = WS("/stream/pull", wsClient.flow)

            req ~> websocketRoute ~> check {
              // check response for WS Upgrade headers
              isWebSocketUpgrade shouldEqual true

              // manually run a WS conversation
              import io.circe.syntax._
              val fromClient = clientRequest.asJson

              wsClient.sendMessage(fromClient.noSpaces)
              val received = (0 until expectedData.size).map { _ =>
                wsClient.expectMessage()
              }
              received.size shouldBe expectedData.size

              val textReceived = received.map(_.asTextMessage.getStrictText)
              val messages = textReceived.map { json =>
                decode[RecordJson](json).right.get
              }
              messages.size shouldBe expectedData.size
              messages.foreach { msg =>
                msg.topic shouldBe topic
              }
              messages.map(_.key) should contain theSameElementsAs (expectedData)
              messages.map(_.offset) should contain theSameElementsAs (expectedData.indices)

              wsClient.sendCompletion()
              //          wsClient.expectCompletion()
            }
          }
        } finally {
          schedulerService.shutdown()
        }
      }
    }
  }
}
