package geometry

import geometry.Plot.Layout.AsciiRectangle

final case class Rectangle private (topLeft: Point, bottomRight: Point) {
  def x1                     = topLeft.x
  def y1                     = topLeft.y
  def x2                     = bottomRight.x
  def y2                     = bottomRight.y
  def contains(point: Point) = containsX(point.x) && containsY(point.y)
  def containsX(xValue: Double) = {
    xValue >= topLeft.x && xValue <= bottomRight.x
  }
  def containsY(yValue: Double) = {
    yValue <= topLeft.y && yValue >= bottomRight.y
  }

  /** @param other the other rectangle from which to build a new rectangle from which
    * @return a rectangle which contains both this and the other rectangle
    */
  def max(other: Rectangle): Rectangle = {
    Rectangle(x1.min(other.x1), y1.max(other.y1), x2.max(other.x2), y2.min(other.y2))
  }

  def splitVertically(): (Rectangle, Rectangle) = {
    val half = width / 2
    val a    = Rectangle(topLeft, bottomRight.copy(x = x1 - half))
    val b    = Rectangle(topLeft.copy(x = x1 + half), bottomRight)
    (a, b)
  }
  def splitHorizontally(): (Rectangle, Rectangle) = {
    val half = height / 2
    val a    = Rectangle(topLeft, bottomRight.copy(y = y2 - half))
    val b    = Rectangle(topLeft.copy(y = y1 + half), bottomRight)
    (a, b)
  }

  def topRight: Point   = topLeft.copy(x = bottomRight.x)
  def bottomLeft: Point = topLeft.copy(y = bottomRight.y)

  def topEdge    = LineSegment(topLeft, topRight)
  def rightEdge  = LineSegment(topRight, bottomRight)
  def bottomEdge = LineSegment(bottomRight, bottomLeft)
  def leftEdge   = LineSegment(bottomLeft, topLeft)
  def edges      = List(topEdge, rightEdge, bottomEdge, leftEdge)

  def clipX(otherX: Double)                                      = clipValue(x1, x2, otherX)
  def clipY(otherY: Double)                                      = clipValue(y2, y1, otherY)
  private def clipValue(min: Double, max: Double, value: Double) = value.max(min).min(max)

  /** @param other the rectangle to clip based on this bounds of this rectangle
    * @return the part of the 'other' rectangle which is contained within this rectangle (or None if none of it is)
    */
  def clip(other: Rectangle): Option[Rectangle] = {
    if (other.x2 < x1 || other.x1 > x2) {
      None
    } else if (other.y2 > y1 || other.y1 < y2) {
      None
    } else {
      // there's some overlap
      val clippedTopLeft     = Point(clipX(other.x1), clipY(other.y1))
      val clippedBottomRight = Point(clipX(other.x2), clipY(other.y2))
      Option(Rectangle(clippedTopLeft, clippedBottomRight))
    }
  }
  def ascii(): Plot.AsciiRows = {
    ascii('-', '|', '-', '|', '+', this)
  }

  def ascii(char: Char): Plot.AsciiRows = ascii(char, char, char, char, char, this)

  def ascii(topChar: Char, rightChar: Char, bottomChar: Char, leftChar: Char, cornerChar: Char, box: Rectangle): Plot.AsciiRows = {
    new AsciiRectangle(topChar, rightChar, bottomChar, leftChar, cornerChar).layout(this, box)
  }

  def pretty(): String = ascii().toString

  def translate(deltaX: Double = 0, deltaY: Double = 0): Rectangle = {
    if (deltaX == 0 && deltaY == 0) {
      this
    } else {
      copy(topLeft.translate(deltaX, deltaY), bottomRight.translate(deltaX, deltaY))
    }
  }

  def width: Double  = x2 - x1
  def height: Double = y1 - y2

  def scale(deltaX: Double = 1.0, deltaY: Double = 1.0): Rectangle = {
    val newW = width * deltaX
    val newH = height * deltaY

    val shiftX = (width - newW) / 2
    val shiftY = (height - newH) / -2

    val tl = Point(x1 + shiftX, y1 + shiftY)
    val br = Point(tl.x + newW, tl.y - newH)
    Rectangle(tl, br)
  }

//  def centerOnOrigin: Rectangle = translate(-topLeft.x - (width / 2), -topLeft.y + (height / 2))
}

object Rectangle {
  def apply(x1: Double, y1: Double, x2: Double, y2: Double): Rectangle = {
    apply(Point(x1, y1), Point(x2, y2))
  }
  def apply(a: Point, b: Point): Rectangle = {
    val topLeft     = Point(a.x.min(b.x), a.y.max(b.y))
    val bottomRight = Point(a.x.max(b.x), a.y.min(b.y))
    new Rectangle(topLeft, bottomRight)
  }
}
