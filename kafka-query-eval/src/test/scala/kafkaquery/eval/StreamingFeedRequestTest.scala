package kafkaquery.eval

import io.circe.syntax._
import kafkaquery.kafka.{CancelFeedRequest, QueryRequest, Rate, StreamStrategy, StreamingFeedRequest, UpdateFeedRequest}
import org.scalatest.{Matchers, WordSpec}

class StreamingFeedRequestTest extends WordSpec with Matchers {

  "StreamingFeedRequest" should {
    List[StreamingFeedRequest](
      CancelFeedRequest,
      UpdateFeedRequest(QueryRequest("a", "b", "c", "d", Option("earliest"), Option(Rate.perSecond(123)), StreamStrategy.Latest))
    ).foreach { expected =>
      s"marshal $expected to/from json" in {
        expected.asJson.as[StreamingFeedRequest] shouldBe Right(expected)
      }
    }
  }
}
