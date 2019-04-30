package pipelines.client.jvm

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import pipelines.users.{LoginRequest, LoginResponse}

import scala.util.Try

class PipelinesClientTest extends WordSpec with Matchers with ScalaFutures {

  "PipelinesClient" should {
    "work" in {
      val client                        = PipelinesClient("https://localhost:80")
      val response1: Try[LoginResponse] = client.login(LoginRequest("admin", "password"))
      println(response1)
      val response2: Try[LoginResponse] = client.login(LoginRequest("admin", "wrong"))
      println(response2)

    }
  }
}
