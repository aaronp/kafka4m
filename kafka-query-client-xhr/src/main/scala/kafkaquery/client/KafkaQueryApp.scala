package kafkaquery.client

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw.{Element, HTMLInputElement, HTMLTextAreaElement}

import scala.scalajs.js.annotation.JSExportTopLevel

/**
  * Examples from
  * http://scala-js.github.io/scala-js-dom/
  */
object KafkaQueryApp extends SupportFunctions with KafkaFunctions with HtmlUtils {

  @JSExportTopLevel("valueOf")
  def valueOf(id: String): String = {
    val elm: Element = document.getElementById(id)
    val result = elm match {
      case x: HTMLTextAreaElement => x.value
      case x: HTMLInputElement    => x.value
      case other =>
        showAlert(s"valueOf('$id') was ${other}")
        sys.error(s"valueOf('$id') was ${other}")
    }
    dom.window.console.log(s"valueOf('$id') was ${result}")
    result
  }

  @JSExportTopLevel("publish")
  def publish(topicId: String, keyId: String, dataId: String) = {
    support.publishData(valueOf(topicId), valueOf(keyId), valueOf(dataId))
  }

  @JSExportTopLevel("listTopics")
  def listTopics(): Unit = kafka.listTopics()

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = appendPar(document.body, "You clicked the button!")

}
