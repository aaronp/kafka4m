package pipelines.data.shape

import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class ComputeTest extends WordSpec with Matchers {


  "Compute" should {

    "compose two functions" in {
      def three[A](x: A)        = List(x, x, x)
      def sizeOf(list: List[_]) = list.size
      val computeSize = Compute
        .partial {
          case ParameterizedShape("List", Seq(_)) => Shape("Int")
        }
        .of(sizeOf)

      val triple      = Compute.lift(Shape("List", _))(three)
      val alwaysThree = triple andThen computeSize
      computeSize.outputFor(Shape("Any")) shouldBe None
      computeSize.outputFor(Shape("List", "whatever")) shouldBe Some(Shape("Int"))
      alwaysThree.outputFor(Shape("whatever")) shouldBe Some(Shape("Int"))

      alwaysThree.applyUnsafe(123) shouldBe 3
      alwaysThree.applyUnsafe("hi") shouldBe 3
    }
    "return List[(Int, String)] for a function which returns List[(A,B)]" in {
      def listSwap[A, B](inputs: List[(A, B)]) = {
        inputs.map(_.swap)
      }

      val calc = Compute
        .partial {
          case ParameterizedShape("List", Seq(a, b)) => Shape("List", b, a)
        }
        .of(listSwap)

      calc.toString shouldBe "Partial[List -> List]"
      val input = List("hi", "there").zipWithIndex
      calc.applyUnsafe(input) shouldBe input.map(_.swap)
    }

    "return Try[String] for a function which returns Try[A]" in {
      def asTry[A](x: A) = Try(x)
      val c              = Compute.lift(Shape("Try", _))(asTry)
      c.outputFor(Shape("String")) shouldBe Some(Shape("Try", Shape("String")))
      c.outputFor(Shape("Int")) shouldBe Some(Shape("Try", Shape("Int")))
      c.applyUnsafe(1) shouldBe Try(1)
      c.applyUnsafe("hi") shouldBe Try("hi")
    }
    "calculate a Try[Int] for a String" in {
      Compute((s: String) => Try(s.toInt)).outputFor(Shape("String")) shouldBe Some(Shape("Try", Shape("Int")))
      Compute((s: String) => Try(s.toInt)).outputFor(Shape("Int")) shouldBe None
    }
    "calculate an Int for a String" in {
      val compute = Compute((_: String).toInt)
      compute.outputFor(Shape("String")) shouldBe Some(Shape("Int"))
      compute.outputFor(Shape("Int")) shouldBe None
    }
  }
}
