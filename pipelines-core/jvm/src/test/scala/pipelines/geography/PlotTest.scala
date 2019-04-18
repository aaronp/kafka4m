package pipelines.geography

import org.scalatest.{Matchers, WordSpec}
import pipelines.geography.Plot.AsciiRow

import Plot.Layout._

class PlotTest extends WordSpec with Matchers {

  "AsciiLine" should {
    "plot vertical from (-5.0, -5.0) to (-5.0, 5.0)" in {
      val vertical             = LineSegment(Point(-5.0, -5.0), Point(-5.0, 5.0))
      val rect                 = vertical.boundingBox
      val rows: Plot.AsciiRows = AsciiLineImpl.layout(vertical, rect)
      rows.rows.size shouldBe 11
      rows.rows shouldBe List(
        (-5, AsciiRow(List((-5, 'o')))),
        (-4, AsciiRow(List((-5, 'o')))),
        (-3, AsciiRow(List((-5, 'o')))),
        (-2, AsciiRow(List((-5, 'o')))),
        (-1, AsciiRow(List((-5, 'o')))),
        (0, AsciiRow(List((-5, 'o')))),
        (1, AsciiRow(List((-5, 'o')))),
        (2, AsciiRow(List((-5, 'o')))),
        (3, AsciiRow(List((-5, 'o')))),
        (4, AsciiRow(List((-5, 'o')))),
        (5, AsciiRow(List((-5, 'o'))))
      )
      val text = rows.toString.lines.toList

      text.size shouldBe 11
      text.foreach(_ should startWith("o"))
    }
    "plot vertical lines" in {
      val vertical = LineSegment(Point(5.0, 5.0), Point(5.0, -5.0))

      val rows: Plot.AsciiRows = AsciiLineImpl.layout(vertical, Rectangle(0, 5, 5, -5))
      rows.rows.size shouldBe 11
      rows.rows shouldBe List(
        (-5, AsciiRow(List((5, 'o')))),
        (-4, AsciiRow(List((5, 'o')))),
        (-3, AsciiRow(List((5, 'o')))),
        (-2, AsciiRow(List((5, 'o')))),
        (-1, AsciiRow(List((5, 'o')))),
        (0, AsciiRow(List((5, 'o')))),
        (1, AsciiRow(List((5, 'o')))),
        (2, AsciiRow(List((5, 'o')))),
        (3, AsciiRow(List((5, 'o')))),
        (4, AsciiRow(List((5, 'o')))),
        (5, AsciiRow(List((5, 'o'))))
      )
      val text = rows.toString.lines.toList

      text.size shouldBe 11
      text.foreach(_ should startWith("o"))
    }
    "plot the vertical line (5.0,-5.0) to (-5.0,-5.0)" in {
      val vertical             = LineSegment(Point(5.0, -5.0), Point(5.0, 5.0))
      val rect                 = vertical.boundingBox
      val rows: Plot.AsciiRows = AsciiLineImpl.layout(vertical, rect)
      rows.rows.size shouldBe 11
      rows.rows shouldBe List(
        (-5, AsciiRow(List((5, 'o')))),
        (-4, AsciiRow(List((5, 'o')))),
        (-3, AsciiRow(List((5, 'o')))),
        (-2, AsciiRow(List((5, 'o')))),
        (-1, AsciiRow(List((5, 'o')))),
        (0, AsciiRow(List((5, 'o')))),
        (1, AsciiRow(List((5, 'o')))),
        (2, AsciiRow(List((5, 'o')))),
        (3, AsciiRow(List((5, 'o')))),
        (4, AsciiRow(List((5, 'o')))),
        (5, AsciiRow(List((5, 'o'))))
      )
      val text = rows.toString.lines.toList

      text.size shouldBe 11
      text.foreach(_ should startWith("o"))
    }
    "plot the horizontal line (5.0,-5.0) to (-5.0,-5.0)" in {
      val horizontal = LineSegment(Point(5.0, -5.0), Point(-5.0, -5.0))

      val rect                 = horizontal.boundingBox
      val rows: Plot.AsciiRows = AsciiLineImpl.layout(horizontal, rect)
      rows.rows shouldBe List(-5 -> AsciiRow((-5 to 5).map(_ -> 'o').toList))
    }
    "plot vertical lines within larger boxes" in {

      val box  = Rectangle(-10, 10, 10, -10)
      val line = LineSegment(5, 5, 5, -5)

      val rows: Plot.AsciiRows = AsciiLineImpl.layout(line, box)
      rows.rows.size shouldBe (11)
      rows.rows shouldBe List(
        (-5, AsciiRow(List((5, 'o')))),
        (-4, AsciiRow(List((5, 'o')))),
        (-3, AsciiRow(List((5, 'o')))),
        (-2, AsciiRow(List((5, 'o')))),
        (-1, AsciiRow(List((5, 'o')))),
        (0, AsciiRow(List((5, 'o')))),
        (1, AsciiRow(List((5, 'o')))),
        (2, AsciiRow(List((5, 'o')))),
        (3, AsciiRow(List((5, 'o')))),
        (4, AsciiRow(List((5, 'o')))),
        (5, AsciiRow(List((5, 'o'))))
      )
    }
  }
  "AsciiRectangle" ignore {

    "plot rectangles" in {

      val r1 = Rectangle(-5, 5, 5, -5)
      println(r1)
//      val rows: Plot.AsciiRows = AsciiRectangle.layout(r1, r1)
//      println(rows)
//      println()
      val box = Rectangle(-10, 10, 10, -10)
      println()
      println(s"r1: $r1")
      println(s"box: $box")
      println()
      val rows2: Plot.AsciiRows = AsciiRectangle.layout(r1, box)
      println()
      println(rows2)
      println()
    }
  }
}
