package pipelines.eval

import io.circe.syntax._
import pipelines.kafka.{CancelFeedRequest, QueryRequest, ResponseFormat, StreamingFeedRequest, UpdateFeedRequest}
import org.scalatest.{Matchers, WordSpec}
import pipelines.core.{Rate, StreamStrategy}

class StreamingFeedRequestTest extends WordSpec with Matchers {

  val request = new QueryRequest(
    clientId = "clientId",
    groupId = "groupId",
    topic = "topic",
    filterExpression = "filterExpression",
    filterExpressionIncludeMatches = true,
    fromOffset = Option("latest"),
    messageLimit = Option(Rate.perSecond(123)),
    format = Option(ResponseFormat(List("key", "foo"))),
    streamStrategy = StreamStrategy.Latest
  )

  "StreamingFeedRequest" should {
    List[StreamingFeedRequest](
      CancelFeedRequest,
      UpdateFeedRequest(request)
    ).foreach { expected =>
      s"marshal $expected to/from json" in {
        expected.asJson.as[StreamingFeedRequest] shouldBe Right(expected)
      }
    }
  }
}
