package pipelines.expressions

import org.scalatest.{Matchers, WordSpec}
import pipelines.expressions.RichOptionalTest.SomeData

import scala.util.Success

class JsonExpressionsTest extends WordSpec with Matchers {

  "JsonExpression" should {
    "be able to evaluate some json expression against itself" in {
      // see RichOptionalTest from which this expression was taken:
      val expressions        = """((value.dbl < 1000) && (value.id == "abc"))"""
      val cache              = JsonExpressions.newCache
      val Success(predicate) = cache(expressions)

      import io.circe.generic.auto._
      import io.circe.syntax._
      predicate(SomeData(id = "abc", lng = 123, dbl = 456.789).asJson) shouldBe true
      predicate(SomeData(id = "abc", lng = 123, dbl = 1001).asJson) shouldBe false
      predicate(SomeData(id = "foo", lng = 123, dbl = 456.789).asJson) shouldBe false

    }
  }
}
