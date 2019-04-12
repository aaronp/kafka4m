package pipelines.kafka

import org.scalatest.{Matchers, WordSpec}
import io.circe.syntax._

class QueryRequestTest extends WordSpec with Matchers {

  "StreamingFeedRequest" should {
    s"marshal ${QueryRequestTest.expected} to/from json" in {
      QueryRequestTest.expected.asJson.as[QueryRequest] shouldBe Right(QueryRequestTest.expected)
    }
  }
}

object QueryRequestTest {
  val expected: QueryRequest = new QueryRequest(
    clientId = "clientId",
    groupId = "groupId",
    topic = "topic",
    filterExpression = "filterExpression",
    filterExpressionIncludeMatches = true,
    fromOffset = Option("latest"),
    messageLimit = Option(Rate.perSecond(123)),
    format = Option(ResponseFormat(List("key", "foo"))),
    streamStrategy = StreamStrategy.Latest)


}
