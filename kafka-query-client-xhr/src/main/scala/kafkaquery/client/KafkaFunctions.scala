package kafkaquery.client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportTopLevel

trait KafkaFunctions extends HtmlUtils {

  object kafka {

    def listTopics(): Unit = {
      val topics = KafkaQueryXhrClient.listTopics.listTopicsEndpoint()
      topics.onComplete {
        case result => showAlert(s"ListTopics returned: ${result}")
      }
    }
  }
}
