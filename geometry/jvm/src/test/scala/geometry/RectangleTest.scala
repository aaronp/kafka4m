package geometry

import org.scalatest.{Matchers, WordSpec}

class RectangleTest extends WordSpec with Matchers {

  "Rectangle.scale" should {
    "scale" in {
      Rectangle(-1, 1, 1, -1).scale(5, 5) shouldBe Rectangle(-5, 5, 5, -5)
    }
  }
  "Rectangle.max" should {
    "return the max of two rectangles" in {
      Rectangle(-2, 2, 2, -2).max(Rectangle(-1, 1, 1, -1)) shouldBe Rectangle(-2, 2, 2, -2)
      Rectangle(-2, 2, 2, -2).max(Rectangle(-1, 3, 5, -1)) shouldBe Rectangle(-2, 3, 5, -2)
    }
  }
  "Rectangle.clip" should {
    val tenRectCenteredAtOrigin = Rectangle(-5, 5, 5, -5)
    val r                       = tenRectCenteredAtOrigin
    "clip to the top-left" in {
      val t = r.translate(-1, 1)
      t shouldBe Rectangle(-6, 6, 4, -4)
      t.clip(r) shouldBe Some(Rectangle(-5, 5, 4, -4))
    }
    "clip to the bottom right" in {
      val t = r.translate(1, -1)
      t shouldBe Rectangle(-4, 4, 6, -6)
      t.clip(r) shouldBe Some(Rectangle(-4, 4, 5, -5))
    }
    "remote rectangles completely outside" in {
      val t = r.translate(-11, 0)
      t shouldBe Rectangle(-16, -5, -6, 5)
      t.clip(r) shouldBe None
      Rectangle(-1, -7, 2, -8).clip(r) shouldBe None
    }
    "leave rectangles completely inside untouched" in {
      Rectangle(-16, -5, 6, 5).clip(r) shouldBe Some(r)
      Rectangle(0, 0, 10, 10).clip(Rectangle(1, 1, 9, 9)) shouldBe Some(Rectangle(1, 1, 9, 9))
      Rectangle(1, 1, 9, 9).clip(Rectangle(0, 0, 10, 10)) shouldBe Some(Rectangle(1, 1, 9, 9))
    }
  }
}
