package pipelines.data.shape

import io.circe.Json
import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class ShapeTest extends WordSpec with Matchers {

  import ShapeTest._

  "Shape" should {
    "work out doubly nested types" in {
      Shape.of[Try[Try[Int]]] shouldBe ParameterizedShape("Try", ParameterizedShape("Try", SimpleShape("Int") :: Nil) :: Nil)
    }
    "work out arrays" in {
      Shape.of[Array[Byte]] shouldBe ParameterizedShape("Array", SimpleShape("Byte") :: Nil)
    }
    "work out tuples" in {
      Shape.of[(Int, List[String])] shouldBe ParameterizedShape("Tuple2", SimpleShape("Int") :: ParameterizedShape("List", SimpleShape("String") :: Nil) :: Nil)
    }
    "work out parameterized types" in {
      Shape.of[Custom[Json]] shouldBe ParameterizedShape("Custom", SimpleShape("Json") :: Nil)
    }
    "work out existential types" in {
      Shape.of[Try[_]] shouldBe ParameterizedShape("Try", ParamShape("_$1") :: Nil)
    }
  }
}

object ShapeTest {
  case class Custom[T](value: T)
}
