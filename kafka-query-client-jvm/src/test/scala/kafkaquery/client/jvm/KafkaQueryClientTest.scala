package kafkaquery.client.jvm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import kafkaquery.kafka.ListTopicsResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

class KafkaQueryClientTest extends WordSpec with Matchers with ScalaFutures {

  "KafkaQueryClient" ignore {
    "work" in {
      implicit val actorSystem = ActorSystem("test")
      implicit val mat         = ActorMaterializer()
      try {
        val client                              = KafkaQueryClient("localhost", 80, 2.seconds)
        val results: Future[ListTopicsResponse] = client.listTopics.listTopicsEndpoint()
        val topics                              = results.futureValue
        topics should not be (null)
      } finally {
        actorSystem.terminate().futureValue
      }
    }
  }
}
