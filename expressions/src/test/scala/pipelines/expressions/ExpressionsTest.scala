package pipelines

package expressions

import example.Example
import org.scalatest.{Matchers, WordSpec}

import scala.language.dynamics

class ExpressionsTest extends WordSpec with Matchers {

  def newData(): Example.Builder = {
    example.Example.newBuilder().setId("foo").setSomeDouble(12.34).setSomeLong(6).setSomeText("hello")
  }

  "parseRule" should {
    "be able to supply arbitrary bodies" in {

      val matching = parseRule("""value.someInt > 80 && value.someInt < 100 || (value.someInt > value.someLong)""")
      matching(newData.setSomeInt(90).build) shouldBe true
      matching(newData.setSomeInt(1000).setSomeLong(456).build) shouldBe true
      matching(newData.setSomeInt(455).setSomeLong(456).build) shouldBe false
    }
  }
  "Expression" should {
    "evaluate general expressions" in {
      val thisCompilesAsAnExpression = "value.someText.asString.forall(_.isDigit)"
      val test                       = Expressions.Predicate(thisCompilesAsAnExpression)
      val input                      = newData.setSomeText("123").build
      test(input) shouldBe true
    }
  }

}
