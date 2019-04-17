package pipelines.geography

import pipelines.geography.Plot.Layout.{AsciiLine, AsciiLineImpl, AsciiPoint, AsciiPolygon}

object Polygon {
  def apply(first: Point, theRest: Point*): Polygon = new Polygon(first +: theRest)
}

case class Polygon(points: Seq[Point]) {

  /**
    * If we have a polygon w/ lots of points, we can hopefully remove some points which lie midway between two other similar points)
    *
    * @param slopeThreshold the percentage
    * @return
    */
  def reduce(slopeThreshold: Double) = {
    // TODO
    this
  }

  def pretty(): String = ascii().toString

  def ascii(view: Rectangle = boundingBox) = AsciiPolygon.layout(this, view)

  def boundingBox: Rectangle = {
    if (points.isEmpty) {
      Rectangle(0, 0, 0, 0)
    } else {
      val x  = points.map(_.x).min
      val y  = points.map(_.y).min
      val x2 = points.map(_.x).max
      val y2 = points.map(_.y).max
      Rectangle(x, y, x2, y2)
    }
  }

  /** @param point
    * @return true if this polygon contains the point
    */
  def contains(point: Point): Boolean = {
    val rayToInfinity = LineSegment(point, Point(Double.MaxValue, Double.MaxValue))

    // if we intersect at a corner,
    val intersectPoints: List[Point] = edges.flatMap(rayToInfinity.intersect).toList
    val uniquePoints: Set[Point]                = intersectPoints.toSet

    uniquePoints.size % 2 == 1
  }

  def edges: TraversableOnce[LineSegment] = {
    points match {
      case Seq()  => Nil
      case Seq(_) => Nil
      case many @ (head +: _) => {
        val s: Iterator[Seq[Point]] = many.sliding(2, 1)
        s.flatMap {
          case Seq(a, b) => Option(LineSegment(a, b))
          case Seq(last) => Option(LineSegment(last, head))
          case Seq()     => Nil
          case other     => sys.error(s"wtf? $other")
        }
      }
    }
  }
}
