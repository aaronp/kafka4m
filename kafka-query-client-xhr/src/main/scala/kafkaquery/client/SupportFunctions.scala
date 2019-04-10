package kafkaquery.client

import kafkaquery.kafka.PublishMessage
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportTopLevel

trait SupportFunctions extends HtmlUtils {

  object support {

    def publishData(topic: String, key: String, data: String): Unit = {
      val request = PublishMessage(topic, key, data)
      dom.window.console.log(s"Publishing $request")
      KafkaQueryXhrClient.publish.publishEndpoint(request).onComplete {
        case result =>
          showAlert(s"publish returned: ${result}")
      }
    }
  }
}
