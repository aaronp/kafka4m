package kafkaquery.kafka

import org.scalatest.{Matchers, WordSpec}
import io.circe.syntax._

class QueryRequestTest extends WordSpec with Matchers {

  "StreamingFeedRequest" should {
    val expected: QueryRequest = QueryRequest("a", "b", "c", "d", Option("latest"), Option(Rate.perSecond(123)), StreamStrategy.Latest)
    s"marshal $expected to/from json" in {
      expected.asJson.as[QueryRequest] shouldBe Right(expected)
    }
  }
}
