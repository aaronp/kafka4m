package pipelines.client.jvm

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import pipelines.users.LoginRequest

class PipelinesClientTest extends WordSpec with Matchers with ScalaFutures {

  "PipelinesClient" should {
    "work" in {
      val client   = PipelinesClient("localhost:80")
      val response = client.login(LoginRequest("user", "name"))

    }
  }
}
