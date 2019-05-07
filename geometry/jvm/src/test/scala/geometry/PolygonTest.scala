package geometry

import org.scalatest.{Matchers, WordSpec}
import geometry.Plot.Layout.{AsciiLine, AsciiLineImpl, AsciiPoint, AsciiPolygon, AsciiPolygonImpl}

class PolygonTest extends WordSpec with Matchers {

  "Polygon.clip" should {}
  "Polygon.contains" should {
    "return true for points inside the polygon" in {

      /**
        *
        * (0,10)
        *              (10,10)
        *    a-b    e---f
        *    | |    |   |
        *    | | x  | y |  z
        *    | |    |   |
        *    | c----d   |
        *    |          |
        *    |          |
        *    h----------g
        * (0,0)        (10,0)
        */
      val a = Point(0, 10)
      val b = Point(2, 10)
      val c = Point(2, 5)
      val d = Point(8, 5)
      val e = Point(8, 10)
      val f = Point(10, 10)
      val g = Point(10, 0)
      val h = Point(0, 0)

      val x = Point(3, 7)
      val y = Point(9, 7)
      val z = Point(11, 7)
      val polygon = Polygon(
        a,
        b,
        c,
        d,
        e,
        f,
        g,
        h,
        a
      )

      def check(point: Point, expected: Boolean) = {

        val rayToInfinity = LineSegment(point, Point(Double.MaxValue, Double.MaxValue))

        lazy val help = {
          val box      = polygon.boundingBox.scale(1.5, 1.5)
          val rayAscii = new AsciiLine('r').layout(rayToInfinity, box)
          val plot     = AsciiPolygonImpl.layout(polygon, box).merge(rayAscii)

          val intersectPlot = polygon.edges
            .filter(rayToInfinity.intersects)
            .foldLeft(plot) {
              case (p, e) =>
                val edgeAscii = new AsciiLine('!').layout(e, box)
                p.merge(edgeAscii)
            }
            .merge(new AsciiPoint('?').layout(point, box))

          intersectPlot.toString
        }

        if (expected != polygon.contains(point)) {
          withClue(s"\n$help\n") {
            polygon.contains(point) shouldBe expected
          }
        }
        polygon.contains(point) shouldBe expected
      }

      check(Point(5, 3), true)

      polygon.points.foreach { p =>
        withClue(p.toString) {
          check(p, true)
        }
      }
      check(Point(11, 3), false)
      check(x, false)
      check(y, true)
      check(z, false)
    }
  }
}
