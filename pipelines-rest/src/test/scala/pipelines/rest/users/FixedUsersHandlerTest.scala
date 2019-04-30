package pipelines.rest.users

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import pipelines.users.LoginRequest

class FixedUsersHandlerTest extends WordSpec with Matchers with ScalaFutures {
  "FixedUsersHandler" should {
    "work w/ users set in a configuration" in {
      val handler = LoginHandler(ConfigFactory.parseString(s"""
          |pipelines.users.loginHandler=${classOf[FixedUsersHandler].getName}
          |pipelines.users.fixed : {
          |  bob : bobsPassword
          |  felix : foo
          |  admin : hi
          |}
        """.stripMargin))

      handler.login(LoginRequest("bob", "bobsPassword")).futureValue.isDefined shouldBe true
      handler.login(LoginRequest("bob", "invalid")).futureValue.isDefined shouldBe false
    }
  }
}
