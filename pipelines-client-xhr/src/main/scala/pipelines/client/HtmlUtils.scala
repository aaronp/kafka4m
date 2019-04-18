package pipelines.client

import java.util.UUID

import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLInputElement, HTMLTextAreaElement}
import org.scalajs.dom.{document, html, window}

import scala.util.control.NonFatal

object HtmlUtils extends HtmlUtils

trait HtmlUtils {

  def showAlert(text: String): Unit = {
    dom.window.alert(text)
  }

  def valueOfNonEmpty(id: String, uniqueId: => String = UUID.randomUUID.toString): String = {
    document.getElementById(id) match {
      case x: HTMLTextAreaElement =>
        Option(x.value).map(_.trim).filterNot(_.isEmpty).getOrElse {
          val default = uniqueId
          x.value = default
          default
        }
      case x: HTMLInputElement =>
        Option(x.value).map(_.trim).filterNot(_.isEmpty).getOrElse {
          val default = uniqueId
          x.value = default
          default
        }
      case other =>
        sys.error(s"valueOf('$id') was ${other}")
        uniqueId
    }
  }

  def valueOf(id: String, elm: Element = null): String = {
    try {
      val result = Option(elm).getOrElse(document.getElementById(id)) match {
        case x: HTMLTextAreaElement => x.value
        case x: HTMLInputElement    => x.value
        case other =>
          sys.error(s"valueOf('$id') was ${other}")
      }
      result
    } catch {
      case NonFatal(err) =>
        dom.window.console.log(s"Couldn't get value for '$id': $err")
        ""
    }
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode  = dom.document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  def mouseMove(pre: html.Pre) = {
    pre.onmousemove = { (e: dom.MouseEvent) =>
      pre.textContent = s"""e.clientX ${e.clientX}
                           |e.clientY ${e.clientY}
                           |e.pageX   ${e.pageX}
                           |e.pageY   ${e.pageY}
                           |e.screenX ${e.screenX}
                           |e.screenY ${e.screenY}
         """.stripMargin
    }
  }

  def base64EncodeInputToDiv(in: html.Input, out: html.Div) = {
    in.onkeyup = { (e: dom.Event) =>
      out.textContent = window.btoa(in.value)
    }
  }
}
