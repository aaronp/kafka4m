package geometry

import org.scalatest.{Matchers, WordSpec}

class SlopeTest extends WordSpec with Matchers {

  "Slope - Slope" should {
    "return -5/2 for -2/1 - 1/2" in {
      (Slope(-2, 1) - Slope(1, 2)) shouldBe Slope(-5, 2)
    }
  }
  "Slope.apply" should {
    "produce 1/2 from 0.5" in {
      Slope(0.5) shouldBe Fraction(1, 2)
    }
    s"produce 1/3 from 0.3333" in {
      val input = 1.0 / 3.0
      Slope(input) shouldBe Slope(1, 3)
    }
  }
  "Slope.reduce" should {
    (1 to 13).foreach { i =>
      s"produce 1/2 from $i/${i * 2}" in {
        Fraction(i, i * 2).reduce shouldBe Fraction(1, 2)
      }
      s"produce 4/7 from ${i * 4}/${i * 7}" in {
        Fraction(i * 4, i * 7).reduce shouldBe Fraction(4, 7)
      }
      s"produce 2/3 from ${i * 2}/${i * 3}" in {
        Fraction(i * 2, i * 3).reduce shouldBe Fraction(2, 3)
      }
    }
  }
}
