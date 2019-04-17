package pipelines.client.jvm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pipelines.kafka.ListTopicsResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

class PipelinesClientTest extends WordSpec with Matchers with ScalaFutures {

  "PipelinesClient" should {
    "work" in {
//      implicit val actorSystem = ActorSystem("test")
//      implicit val mat         = ActorMaterializer()
//      try {
//        val client                              = PipelinesClient("localhost", 80, 2.seconds)
//        val results: Future[ListTopicsResponse] = client.listTopics.listTopicsEndpoint()
//        val topics                              = results.futureValue
//        topics should not be (null)
//      } finally {
//        actorSystem.terminate().futureValue
//      }
    }
  }
}
