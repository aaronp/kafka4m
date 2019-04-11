package kafkaquery.rest.routes

import kafkaquery.kafka.{StreamRequest, StreamStrategy}
import kafkaquery.rest.routes.SocketAdapter.WebSocketClientRequest
import org.scalatest.{Matchers, WordSpec}
import io.circe.syntax._

class SocketAdapterTest extends WordSpec with Matchers {
  "SocketAdapter.WebSocketClientRequest" should {
    List[WebSocketClientRequest](
      SocketAdapter.CancelFeed,
      SocketAdapter.UpdateFeed(StreamRequest("a", "b", "c", "d", Option(1), Option(2), StreamStrategy.Latest))
    ).foreach { expected =>
      s"marshal $expected to/from json" in {
        expected.asJson.as[WebSocketClientRequest] shouldBe Right(expected)
      }
    }
  }

}
