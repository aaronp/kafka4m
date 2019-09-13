package kafka4m.util

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class PropsTest extends WordSpec with Matchers {

  "Props.topic" should {
    "get a topic from a config" in {
      val config = ConfigFactory.parseString("""kafka4m.foo.topic = thisIsFoo
          |kafka4m.bar.topic = thisIsBar
          |kafka4m.meh.topic = ""
          |kafka4m.topic = defaultTopic
          |""".stripMargin)

      Props.topic(config, "foo", "bar") shouldBe "thisIsFoo"
      Props.topic(config, "bar") shouldBe "thisIsBar"
      Props.topic(config, "meh") shouldBe "defaultTopic"
    }
  }
}
