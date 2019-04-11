package kafkaquery.client

import kafkaquery.kafka.{QueryRequest, StreamingFeedRequest, UpdateFeedRequest}
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

trait KafkaFunctions extends HtmlUtils {

  object kafka {

    def query(request: QueryRequest) = {
      val json: String = (UpdateFeedRequest(request): StreamingFeedRequest).asJson.noSpaces
      KafkaWSClient(initialHandshakeMessage = json) { msg =>
        // TODO
      }
    }

    def listTopics(): Unit = {
      val topics = KafkaQueryXhrClient.listTopics.listTopicsEndpoint()
      topics.onComplete {
        case result => showAlert(s"ListTopics returned: ${result}")
      }
    }
  }
}
