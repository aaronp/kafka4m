package esa.client.app
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.window
import org.scalajs.dom.html

import scala.scalajs.js.annotation.JSExportTopLevel

/**
  * Examples from
  * http://scala-js.github.io/scala-js-dom/
  *
  */
object Main {

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = {
    appendPar(document.body, "You clicked the button!")
  }

  @JSExportTopLevel("showAlert")
  def showAlert(): Unit = {
    dom.window.alert("Hi from Scala-js-dom")
  }

  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode  = dom.document.createElement("p")
    val textNode = document.createTextNode(text)
    parNode.appendChild(textNode)
    targetNode.appendChild(parNode)
  }

  def appendTo(div: html.Div) = {
    val child = document.createElement("div")
    child.textContent = "Hi from Scala-js-dom"
    div.appendChild(child)
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
