package pipelines.client

import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, html}
import pipelines.geography._

import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js.annotation.JSExport

//@JSExportTopLevel("DrawApp")
/**
  * see https://github.com/lihaoyi/workbench-example-app/tree/dodge-the-dots
  * http://www.lihaoyi.com/hands-on-scala-js/
  * https://www.w3resource.com/html5-canvas/html5-canvas-lines.php
  */
@JSExport
object DrawApp {
  var lastLogged = 0L

  def drawPolys(canvas: html.Canvas, ctxt: CanvasRenderingContext2D, data: Seq[Polygon], mousePos: Point, highlight: Boolean) = {
    clear(canvas, ctxt)
    data.foreach { poly =>
//      dom.window.console.log(s"checking $poly")
//      val ok = Try(poly.contains(mousePos)).toOption.exists(identity)
      val ok = highlight && poly.contains(mousePos)
//      dom.window.console.log(s"$poly contains $mousePos is $ok")

      draw(canvas, ctxt, poly.points, "red", ok)
    }
  }
  def clear(canvas: html.Canvas, ctxt: CanvasRenderingContext2D) = {
    ctxt.lineWidth = 1
    ctxt.fillStyle = "#f8f8f8"
    ctxt.fillRect(0, 0, canvas.width, canvas.height)
  }
  def draw(canvas: html.Canvas, ctxt: CanvasRenderingContext2D, data: Seq[Point], color: String, highlight: Boolean) = {

    data match {
      case Seq() =>
      case Seq(Point(x, y)) =>
        ctxt.fillRect(x, y, 2, 2)
      case Point(firstX, firstY) +: tail =>
        ctxt.beginPath()
        ctxt.moveTo(firstX, firstY)
        tail.foreach {
          case Point(x, y) => ctxt.lineTo(x, y)
        }
        ctxt.closePath()
        ctxt.stroke()
        if (highlight) {
          ctxt.fillStyle = color
          ctxt.fill()
        }
    }
  }

  @JSExport
  def main(canvas: html.Canvas): Unit = {
    val ctxt: CanvasRenderingContext2D = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    canvas.width = canvas.parentElement.clientWidth
    canvas.height = canvas.parentElement.clientHeight

    var buffer = ArrayBuffer[Point]()
    var polys  = ArrayBuffer[Polygon]()

    val MaxLen       = 200
    var pointCounter = MaxLen
    val rect         = canvas.getBoundingClientRect()

    def addNewPoint(point: Point) = {
      buffer += point
      if (pointCounter > 0) {
        pointCounter = pointCounter - 1
      } else {
        buffer = buffer.tail
      }
    }

    def asPoint(e: dom.MouseEvent) = {
      val x = e.clientX - rect.left
      val y = e.clientY - rect.top
      Point(x, y)
    }

    def resetBuffer() = {
      buffer.clear()
      pointCounter = MaxLen
    }
    var downPoint = Option.empty[Point]
    def isDown    = downPoint.nonEmpty
    canvas.onmousedown = (e: dom.MouseEvent) => {
      resetBuffer()
      val firstPoint = asPoint(e)
      downPoint = Option(firstPoint)
      dom.window.console.log(s"setting first point: $firstPoint")
    }
    canvas.onmouseup = (e: dom.MouseEvent) => {
      // close the loop
      downPoint.foreach { firstPoint =>
        dom.window.console.log(s"fist point: $firstPoint, thisPoint = ${asPoint(e)}")
        addNewPoint(firstPoint)
        polys += Polygon(buffer.toList)
        resetBuffer()
        drawPolys(canvas, ctxt, polys, asPoint(e), true)
      }
      downPoint = None
      dom.window.console.log(s"downPoint is : $downPoint")
    }

    canvas.onmousemove = { (e: dom.MouseEvent) =>
      if (isDown) {
        addNewPoint(asPoint(e))
        drawPolys(canvas, ctxt, Polygon(buffer.toList) +: polys, asPoint(e), false)
      } else {
        drawPolys(canvas, ctxt, polys, asPoint(e), true)
      }

    }
  }
}
