package pipelines.geography

case class LineSegment(from: Point, to: Point) {
  override def toString = {
    s"$from to $to ($slopeInterceptFormula)"
  }

  def x1 = from.x
  def y1 = from.y

  def x2 = to.x
  def y2 = to.y

  /** @param y the y coordinate
    * @return
    */
  def xValueAt(y: Double) = {
    if (slope.isVertical) {
      x1
    } else if (m.isNaN) {
      m
    } else {
      (y - b) / m
    }
  }

  def yValueAt(x: Double) = {
    if (slope.isHorizontal) {
      y1
    } else if (m.isNaN) {
      m
    } else {
      (x * m) + b
    }
  }

  def slopeInterceptFormula = s"y = ${slope}x + $b"

  def slope: Slope = {
    val rise = to.y - from.y
    val run  = to.x - from.x
    if (run == 0.0) {
      Slope(Double.PositiveInfinity)
    } else {
      Slope(rise / run)
    }
  }

  // line formula: y = mx + b
  lazy val b = from.y - (m * from.x)

  def m: Double = slope.value

  /** @param other the other line segment (which is treated as a ray)
    * @return the point at which these two lines would intersect if they were infinite rays
    */
  def intersectPoint(other: LineSegment): Option[Point] = {
    val rhs = other.b - b
    if (other.slope.isVertical) {
      if (slope.isVertical) {
        // we could check if it's the same x coord, but that would just get us infinite intersections
        None
      } else {
        // to intersect a vertical line with a non-vertical line, we just plug-in the vertical line's x coord to our formula

        val x = other.from.x.ensuring(_ == other.to.x)
        val y = (m * x) + b
        Option(Point(x, y))
      }
    } else if (slope.isVertical) {
      other.intersectPoint(this)
    } else {
      val multiplier: Slope = (slope - other.slope).reciprocal
      val isParallel        = multiplier.isVertical
      if (isParallel) {
        None
      } else {
        val xResult = multiplier.value * rhs
        val yResult = m * xResult + b
        Option(Point(xResult, yResult))
      }
    }
  }
  def boundingBox = Rectangle(from, to)

  def intersects(other: LineSegment): Boolean = intersect(other).nonEmpty

  def intersect(other: LineSegment): Option[Point] = {
    intersectPoint(other).filter(p => boundingBox.contains(p) && other.boundingBox.contains(p))
  }
}

object LineSegment {
  def apply(x1: Double, y1: Double, x2: Double, y2: Double): LineSegment = {
    new LineSegment(Point(x1, y1), Point(x2, y2))
  }
}
