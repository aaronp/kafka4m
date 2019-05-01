package pipelines.client

import pipelines.kafka.PublishMessage
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.JSExportTopLevel

trait SupportFunctions extends HtmlUtils {

  object support {

    def publishData(topic: String, key: String, data: String): Unit = {
      val request = PublishMessage(topic, key, data)
      dom.window.console.log(s"Publishing $request")
      KafkaQueryXhrClient.publishSupport.publishEndpoint.apply(request).onComplete {
        case result =>
          dom.window.console.log(s"publish returned: ${result}")
      }
    }
  }
}
