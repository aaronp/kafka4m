package pipelines.client

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.raw.Node

import scala.scalajs.js.annotation.JSExportTopLevel

/**
  */
object PipelinesApp extends SupportFunctions with KafkaFunctions with HtmlUtils {

  @JSExportTopLevel("valueOf")
  def valueOf(id: String): String = {
    valueOf(id, document.getElementById(id))
  }

  @JSExportTopLevel("publish")
  def publish(topicId: String, keyId: String, dataId: String) = {
    support.publishData(valueOf(topicId), valueOf(keyId), valueOf(dataId))
  }

  @JSExportTopLevel("renderQuery")
  def renderQuery(queryButtonId: String, resultsDivId: String): Node = {

    import scalatags.JsDom.all._

    val results = dom.window.document.getElementById(resultsDivId)

    val queryButton = button("Query", `class` := "btn").render
    queryButton.onclick = (e: dom.MouseEvent) => {
      e.stopPropagation
      query(
        clientElementElementId = "clientId",
        groupElementId = "groupId",
        topicElementId = "topic",
        filterTextElementId = "query",
        filterExpressionIncludeMatchesId = "filterExpressionIncludeMatchesId",
        offsetElementId = "offset",
        rateLimitElementId = "rate",
        strategyElementId = "strategy",
        isBinaryStreamId = "isBinaryStreamId"
      ) { msg =>
        results.appendChild(div(span(msg.data.toString), br()).render)
      }
    }

    val clearButton = button("Clear", `class` := "btn").render
    clearButton.onclick = (e: dom.MouseEvent) => {
      e.stopPropagation

      results.innerHTML = ""
    }

    dom.window.document.getElementById(queryButtonId).appendChild(div(queryButton, br(), clearButton).render)
  }

  @JSExportTopLevel("listTopics")
  def listTopics(): Unit = kafka.listTopics()

}
