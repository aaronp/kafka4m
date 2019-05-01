package example

import org.scalatest.{Matchers, WordSpec}

class ExampleTest extends WordSpec with Matchers {

  "Example" should {
    "reduce to json" in {
      val d8a: Example =
        Example.newBuilder.setAnEnum(daysOfTheWeek.FRIDAY).setId("123").setSomeDouble(234).setSomeFloat(345).setSomeInt(456).setSomeLong(567).setSomeText("678").build()

      d8a.toString should include("456")
    }
  }
}
