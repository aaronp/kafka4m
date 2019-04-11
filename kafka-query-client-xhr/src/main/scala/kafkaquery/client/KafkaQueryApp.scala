package kafkaquery.client

import kafkaquery.kafka.{QueryRequest, Rate, StreamStrategy}
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.{Element, HTMLInputElement, HTMLTextAreaElement}
import scalatags.JsDom

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Try

/**
  * Examples from
  * http://scala-js.github.io/scala-js-dom/
  */
object KafkaQueryApp extends SupportFunctions with KafkaFunctions with HtmlUtils {

  import dom.window.console

  @JSExportTopLevel("valueOf")
  def valueOf(id: String): String = {
    try {
      val elm: Element = document.getElementById(id)
      val result = elm match {
        case x: HTMLTextAreaElement => x.value
        case x: HTMLInputElement    => x.value
        case other =>
          showAlert(s"valueOf('$id') was ${other}")
          sys.error(s"valueOf('$id') was ${other}")
      }
      console.log(s"valueOf('$id') was ${result}")
      result
    } catch {
      case err =>
        console.log(s"Couldn't get value for '$id': $err")
        ""
    }
  }

  @JSExportTopLevel("publish")
  def publish(topicId: String, keyId: String, dataId: String) = {
    support.publishData(valueOf(topicId), valueOf(keyId), valueOf(dataId))
  }

  @JSExportTopLevel("query")
  def query(clientElementElementId: String,
            groupElementId: String,
            topicElementId: String,
            filterTextElementId: String,
            offsetElementId: String,
            rateLimitElementId: String,
            strategyElementId: String) = {
    val limitOpt = Try(valueOf(rateLimitElementId).toInt).toOption.filter(_ > 0).map { limit =>
      Rate.perSecond(limit)
    }
    val streamStrategy = valueOf(strategyElementId) match {
      case "latest" => StreamStrategy.Latest
      case "all"    => StreamStrategy.All
      case other =>
        console.log(s"Expected latest or all but got '$other'")
        StreamStrategy.Latest
    }
    val request = QueryRequest(
      clientId = valueOf(clientElementElementId),
      groupId = valueOf(groupElementId),
      topic = valueOf(topicElementId),
      filterExpression = valueOf(filterTextElementId),
      fromOffset = Option(valueOf(offsetElementId)).filterNot(_.isEmpty),
      messageLimit = limitOpt,
      streamStrategy = streamStrategy
    )
    kafka.query(request)
  }

  @JSExportTopLevel("renderQuery")
  def renderQuery(id: String) = {

    val divElem = dom.window.document.getElementById(id)

    import scalatags.JsDom.all._

    val btn = button("Query", `class` := "btn").render
    btn.onclick = (e: dom.MouseEvent) => {
      query(
        clientElementElementId = "clientId",
        groupElementId = "groupId",
        topicElementId = "topic",
        filterTextElementId = "query",
        offsetElementId = "offset",
        rateLimitElementId = "rate",
        strategyElementId = "strategy"
      )
    }

    val fragment: JsDom.TypedTag[Div] = div(
      btn
    )

    divElem.appendChild(fragment.render)
  }

  @JSExportTopLevel("listTopics")
  def listTopics(): Unit = kafka.listTopics()

}
