package pipelines.client

import org.scalajs.dom
import org.scalajs.dom.raw.{ClientRect, MouseEvent}
import org.scalajs.dom.{CanvasRenderingContext2D, html}
import geometry._
import org.scalajs.dom
import org.scalajs.dom.document
import scalatags.JsDom.all._
import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js.annotation.JSExport

//@JSExportTopLevel("DrawApp")
/**
  * see https://github.com/lihaoyi/workbench-example-app/tree/dodge-the-dots
  * http://www.lihaoyi.com/hands-on-scala-js/
  * https://www.w3resource.com/html5-canvas/html5-canvas-lines.php
  */
@JSExport
object DrawApp extends HtmlUtils {
  var lastLogged = 0L

  @JSExport
  def removeVertices(thresholdDivId: String): Unit = {
    val PercentR = "(.*)%".r
    val percentThreshold: Double = valueOf(thresholdDivId).trim match {
      case PercentR(percent) => percent.toDouble / 100
      case percent           => percent.toDouble
    }
    //Option(state).foreach(_.trimVertices(percentThreshold))

  }

  var state: State = null

  @JSExport
  def main(canvas: html.Canvas, logDiv: html.Div): Unit = {
    canvas.width = canvas.parentElement.clientWidth
    canvas.height = canvas.parentElement.clientHeight

    state = new State(canvas, logDiv)

    canvas.onmousedown = state.onMouseDown
    canvas.onmouseup = state.onMouseUp
    canvas.onmousemove = state.onMouseMove
  }

  def drawPolys(canvas: html.Canvas, ctxt: CanvasRenderingContext2D, data: Seq[Polygon], mousePos: Point, highlight: Boolean) = {
    clear(canvas, ctxt)
    data.foreach { poly =>
      val ok = highlight && poly.contains(mousePos)
      draw(canvas, ctxt, poly.points, "red", ok)
    }
  }
  def drawLines(canvas: html.Canvas, ctxt: CanvasRenderingContext2D, data: Seq[Point], mousePos: Point) = {
    clear(canvas, ctxt)

    (data :+ mousePos) match {
      case Seq(Point(x, y)) =>
        ctxt.fillRect(x, y, 2, 2)
      case Point(firstX, firstY) +: tail =>
        ctxt.beginPath()
        ctxt.moveTo(firstX, firstY)
        tail.foreach {
          case Point(x, y) => ctxt.lineTo(x, y)
        }
//        ctxt.closePath()
        ctxt.stroke()
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

  class State(canvas: html.Canvas, logDiv: html.Div) {

    private val ctxt: CanvasRenderingContext2D = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    private val rect: ClientRect               = canvas.getBoundingClientRect()

    private var buffer = ArrayBuffer[Point]()
    var downPoint      = Option.empty[Point]

    def onMouseMove(e: MouseEvent) = {
      val mouse = asPoint(e)
      drawLines(canvas, ctxt, buffer, mouse)
      logAngle(mouse)
    }

    def nudge(value: Double) = {
      if (value.isInfinite) {
        Double.MaxValue
      } else if (value == 0) {
        Double.MinValue
      } else {
        value
      }
    }
    def logAngle(point: Point) = {
      buffer.takeRight(2) match {
        case Seq(a, b) =>
          val line1 = LineSegment(a, b)
          val line2 = LineSegment(b, point)

          val slope1 = nudge(line1.slope.value)
          val slope2 = nudge(line2.slope.value)
          val r      = (slope1 - slope2)
//          dom.window.console.log(s"${line1.slopeInterceptFormula} ($slope1) / ${line2.slopeInterceptFormula} ($slope2) is ${r}")
//          dom.window.console.log(s"$r from $slope1 / $slope2")
          dom.window.console.log(s"${line2.slope.value}")
        case _ =>
      }
    }

    def onMouseUp(e: MouseEvent) = {
      downPoint = None
      drawLines(canvas, ctxt, buffer, asPoint(e))
    }

    def onMouseDown(e: MouseEvent) = {
      val firstPoint = asPoint(e)
      downPoint = Option(firstPoint)
      val previousPoint = buffer.lastOption
      buffer += firstPoint

      previousPoint.foreach { before =>
        val line = LineSegment(before, firstPoint)
//        appendPar(logDiv, s"${line}")

        logDiv.appendChild(div(span(s"${line}"), br()).render)
      }
      drawLines(canvas, ctxt, buffer, asPoint(e))
    }

    def isDown = downPoint.nonEmpty

    def asPoint(e: dom.MouseEvent) = {
      val x = e.clientX - rect.left
      val y = e.clientY - rect.top
      Point(x, y)
    }
  }

  class LassoState(canvas: html.Canvas, MaxLen: Int = 200) {

    private val ctxt: CanvasRenderingContext2D = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    private val rect: ClientRect               = canvas.getBoundingClientRect()

    private var buffer       = ArrayBuffer[Point]()
    private var polys        = ArrayBuffer[Polygon]()
    private var pointCounter = MaxLen
    private var downPoint    = Option.empty[Point]

    def trimVertices(percentThreshold: Double) = {
      polys = polys.map { p =>
        val x = p.reduce(percentThreshold, dom.window.console.log(_))

        val s1 = p.points.size
        val s2 = x.points.size

        dom.window.console.log(s"Size before:$s1, size after: $s2")

        x
      }
    }

    def onMouseMove(e: MouseEvent) = {
      if (isDown) {
        addNewPoint(asPoint(e))
        drawPolys(canvas, ctxt, Polygon(buffer.toList) +: polys, asPoint(e), false)
      } else {
        drawPolys(canvas, ctxt, polys, asPoint(e), true)
      }
    }

    def onMouseUp(e: MouseEvent) = {
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
    def onMouseDown(e: MouseEvent) = {
      resetBuffer()
      val firstPoint = asPoint(e)
      downPoint = Option(firstPoint)
      dom.window.console.log(s"setting first point: $firstPoint")
    }
    def addNewPoint(point: Point) = {
      buffer += point
      if (pointCounter > 0) {
        pointCounter = pointCounter - 1
      } else {
        buffer = buffer.tail
      }
    }

    def resetBuffer() = {
      buffer.clear()
      pointCounter = MaxLen
    }
    def isDown = downPoint.nonEmpty
    def asPoint(e: dom.MouseEvent) = {
      val x = e.clientX - rect.left
      val y = e.clientY - rect.top
      Point(x, y)
    }
  }

}
