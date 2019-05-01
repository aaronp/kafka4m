package geometry

import geometry.Plot.Layout.{AsciiPoint, AsciiPolygon}

case class Point(x: Double, y: Double) {
  override def toString = s"($x,$y)"

  def translate(deltaX: Double = 0, deltaY: Double = 0): Point = {
    copy(x + deltaX, y + deltaY)
  }
  def ascii(char: Char = '.', box: Rectangle = Rectangle(this, this)): Plot.AsciiRows = new AsciiPoint(char).layout(this, box)
}
