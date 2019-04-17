package pipelines.geography

import org.scalatest.{Matchers, WordSpec}

class LineSegmentTest extends WordSpec with Matchers {

  "LineSegment.slope" should {
    "return 1/2 for LineSegment(Point(0, 1), Point(2, 2)).slope" in {
      LineSegment(Point(0, 1), Point(2, 2)).slope shouldBe Slope(1, 2)
    }
    "return 0 for a horizontal line" in {
      LineSegment(Point(0, 1), Point(2, 1)).slope shouldBe Slope(0, 1)
    }
    "return Inf for a vertical line" in {
      LineSegment(Point(0, 1), Point(0, 2)).slope shouldBe Slope(Double.PositiveInfinity)
    }
  }
  "LineSegment.slopeInterceptFormula" should {
    "return the y = mx + b form of a line" in {
      LineSegment(Point(0, 1), Point(2, 2)).slopeInterceptFormula shouldBe "y = 1/2x + 1.0"
      LineSegment(Point(0, 2), Point(1, 0)).slopeInterceptFormula shouldBe "y = -2/1x + 2.0"
    }
  }
  "LineSegment.intersects" should {
    "return false for two perpendicular segments which miss each other" in {
      val a = LineSegment(Point(-1, 0), Point(1, 0))
      val b = LineSegment(Point(0, -3), Point(0, -2))
      a.intersects(b) shouldBe false
    }
    "return true for vertical and horizontal lines which cross at the origin" in {
      val a = LineSegment(Point(-1, 0), Point(1, 0))
      val b = LineSegment(Point(0, -1), Point(0, 1))
      a.intersects(b) shouldBe true

      LineSegment(Point(-1, 0), Point(1, 0)).intersects(LineSegment(Point(0, -1), Point(0, 1))) shouldBe true
      LineSegment(Point(-1, 0), Point(1, 0)).intersects(LineSegment(Point(0, 1), Point(0, -1))) shouldBe true
      LineSegment(Point(1, 0), Point(-1, 0)).intersects(LineSegment(Point(0, 1), Point(0, -1))) shouldBe true
    }
  }
  "LineSegment.intersectPoint" should {
    val firstLine  = LineSegment(Point(0, 1), Point(2, 2))
    val secondLine = LineSegment(Point(0, 2), Point(1, 0))

    val expected = Point(2.0 / 5.0, 1.2)
    s"intersect $firstLine with $secondLine at $expected" in {
      firstLine.slope shouldBe Slope(1, 2)
      firstLine.m shouldBe 0.5
      firstLine.slope.reciprocal.value shouldBe 2.0
      secondLine.intersectPoint(firstLine) shouldBe firstLine.intersectPoint(secondLine)
      firstLine.intersectPoint(secondLine) shouldBe Some(expected)
      secondLine.intersectPoint(firstLine) shouldBe Some(expected)
    }
    "intersect a vertical line with a horizontal line" in {
      val a = LineSegment(Point(-1, 0), Point(1, 0))
      val b = LineSegment(Point(0, -1), Point(0, 1))
      a.intersectPoint(b) shouldBe Some(Point(0, 0))
      b.intersectPoint(a) shouldBe Some(Point(0, 0))

      a.intersectPoint(LineSegment(Point(0, -5), Point(0, -4))) shouldBe Some(Point(0, 0))
    }
    "intersect a vertical line with a non-vertical line" in {
      val vertical = LineSegment(Point(0, -1), Point(0, 1))
      val b        = LineSegment(Point(1, 2), Point(3, 4))
      vertical.intersectPoint(b) shouldBe Some(Point(0, 1))
      b.intersectPoint(vertical) shouldBe Some(Point(0, 1))
    }
    "intersect a horizontal line with a non-horizontal line" in {
      val horizontal = LineSegment(Point(-1, 0), Point(1, 0))
      val b          = LineSegment(Point(1, 2), Point(3, 4))
      horizontal.intersectPoint(b) shouldBe Some(Point(-1, 0))
      b.intersectPoint(horizontal) shouldBe Some(Point(-1, 0))
    }
    "not intersect parallel lines" in {
      val line1 = LineSegment(Point(0, 1), Point(2, 2))
      val line2 = LineSegment(Point(2, 2), Point(4, 3))
      line1.intersectPoint(line2) shouldBe None
    }
    "not intersect a line with itself" in {
      val line = LineSegment(Point(0, 1), Point(2, 2))
      line.intersectPoint(line) shouldBe None
    }
  }
}
