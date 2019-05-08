package pipelines.rest.routes

import akka.http.scaladsl.testkit.WSProbe

import concurrent.duration._

class StreamRoutesTest extends BaseRoutesTest {

  "StreamRoutes.websocketPublishRoute" should {
    "create a source which can be subscribed to multiple times" in withScheduler { implicit sched =>
      val streamRoutes = StreamRoutes.dev(100.millis)

      val wsClient = WSProbe()

      val expectedData = (0 to 10).map(i => s"data $i")

      val websocketRoute = streamRoutes.websocketPublishRoute

      WS("/source/create/publish", wsClient.flow) ~> websocketRoute ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        expectedData.foreach(wsClient.sendMessage)

        val expectedHeartbeat = wsClient.expectMessage()
        expectedHeartbeat.asTextMessage.getStrictText shouldBe pipelines.rest.socket.heartBeatTextMsg

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }

    }
  }
  "StreamRoutes backlog" ignore {
    "websocket sources should write down" in withScheduler { implicit sched =>
    }
  }
}
