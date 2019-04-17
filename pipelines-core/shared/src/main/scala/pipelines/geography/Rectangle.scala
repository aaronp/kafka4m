package pipelines.geography

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

  def topRight   = topLeft.copy(x = bottomRight.x)
  def bottomLeft = topLeft.copy(y = bottomRight.y)
  def edges = List(
    LineSegment(topLeft, topRight),
    LineSegment(topRight, bottomRight),
    LineSegment(bottomRight, bottomLeft),
    LineSegment(bottomLeft, topLeft)
  )

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
