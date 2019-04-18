package pipelines.geography

import pipelines.geography.Plot.Layout.{AsciiLine, AsciiLineImpl, AsciiPoint, AsciiPolygon}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object Polygon {
  def apply(first: Point, theRest: Point*): Polygon = new Polygon(first +: theRest)

  @tailrec
  private def append(previous: LineSegment, buffer: ArrayBuffer[Point], allEdges: Iterator[LineSegment], slopeThreshold: Double): Polygon = {
    if (allEdges.hasNext) {
      if (allEdges.hasNext) {
        val nextEdge = allEdges.next()
        val slope1   = previous.slope.value
        val slope2   = nextEdge.slope.value
        val ratio    = slope1 / slope2

        // if the angle between these two edges is < the threshold then we create a new edge w/ just the vertices
        if (ratio < slopeThreshold) {
          buffer += nextEdge.to
        } else {
          buffer += previous.to
        }
        append(nextEdge, buffer, allEdges, slopeThreshold)
      } else {
        // we're on our very last edge - just add the last point
        buffer += previous.to
        Polygon(buffer.toArray)
      }
    } else {
      Polygon(buffer.toArray)
    }
  }
}

case class Polygon(points: Seq[Point]) {

  /**
    * If we have a polygon w/ lots of points, we can hopefully remove some points which lie midway between two other similar points)
    *
    * @param slopeThreshold the percentage
    * @return a polygon w/ some vertices potentially removed
    */
  def reduce(slopeThreshold: Double, log: String => Unit): Polygon = {
    val newPoints: Iterator[Point] = points.sliding(3, 2).flatMap {
      // do we add b and c, or just c
      case Seq(a, b, c) =>
        val line1 = LineSegment(a, b)
        val line2 = LineSegment(b, c)

        val ratio: Double = {
          val slope1 = line1.slope.value
          val slope2 = line2.slope.value
          val r = if (slope2 != 0) {
            slope1 / slope2
          } else {
            Double.PositiveInfinity
          }

          log(s"line1 $line1 slope: ${slope1}, line2 $line2 slope: ${slope2}, ratio: ${r} between $line1 and $line2")
          r
        }

        if (ratio <= slopeThreshold) {
          Seq(a)
        } else {
          Seq(a, b)
        }
      case points => points
    }

    if (newPoints.size == points.size) {
      this
    } else {
      Polygon(newPoints.toSeq)
    }
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
    val uniquePoints: Set[Point]     = intersectPoints.toSet

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
