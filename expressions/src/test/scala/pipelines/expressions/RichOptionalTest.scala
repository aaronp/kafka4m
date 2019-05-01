package pipelines.expressions

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalatest.{Matchers, WordSpec}
import RichOptionalTest._
import io.circe.optics.JsonPath._

class RichOptionalTest extends WordSpec with Matchers with LowPriorityOptionalImplicits {

  "RichOptional expressions" should {
    "be natural" in {
      implicit val json = SomeData(id = "abc", lng = 123, dbl = 456.789).asJson
      val value         = root
      ((value.dbl < 1000) && (value.id ===== "abc")) shouldBe true
    }
  }
  "RichOptional <=" should {
    "return true values <= the target value" in {
      implicit val json = SomeData(lng = 123, dbl = 456.789).asJson
      (root.dbl <= 456.789) shouldBe true
      (root.dbl <= 456.7891) shouldBe true
      (root.dbl <= 456.788) shouldBe false
      (root.lng <= 123) shouldBe true
      (root.lng <= 124) shouldBe true
      (root.lng <= 122) shouldBe false
    }
  }
  "RichOptional >=" should {
    "return true values >= the target value" in {
      implicit val json = SomeData(lng = 123, dbl = 456.789).asJson
      (root.dbl >= 456.789) shouldBe true
      (root.dbl >= 456.7891) shouldBe false
      (root.dbl >= 456.788) shouldBe true
      (root.lng >= 123) shouldBe true
      (root.lng >= 124) shouldBe false
      (root.lng >= 122) shouldBe true
    }
  }

  "RichOptional <" should {
    "return true values < the target value" in {
      implicit val json = SomeData(lng = 123, dbl = 456.789).asJson
      (root.dbl < 456.789) shouldBe false
      (root.dbl < 456.7891) shouldBe true
      (root.dbl < 456.788) shouldBe false
      (root.lng < 123) shouldBe false
      (root.lng < 124) shouldBe true
      (root.lng < 122) shouldBe false
    }
  }
  "RichOptional >" should {
    "return true values > the target value" in {
      implicit val json = SomeData(lng = 123, dbl = 456.789).asJson
      (root.dbl > 456.789) shouldBe false
      (root.dbl > 456.7891) shouldBe false
      (root.dbl > 456.788) shouldBe true
      (root.lng > 123) shouldBe false
      (root.lng > 124) shouldBe false
      (root.lng > 122) shouldBe true
    }
  }
  "RichOptional.=====" should {
    implicit val json: Json = SomeData().asJson
    "work for strings" in {
      (root.id.string ===== "foo") shouldBe true
      (root.id.string ===== "bar") shouldBe false
      (root.id.string ===== 789) shouldBe false
    }
    "work for ints" in {
      (root.reference.number ===== root.value.number) shouldBe true
      (root.reference.number ===== 123) shouldBe true
      (root.reference.number ===== 456) shouldBe false
    }
    "work for longs" in {
      (root.lng.number ===== root.lng.number) shouldBe true
      (root.lng.number ===== root.dbl.number) shouldBe false
      (root.lng.number ===== root.notFound.number) shouldBe false
      (root.lng.number ===== Long.MaxValue) shouldBe true
      (root.lng.number ===== Long.MinValue) shouldBe false
      (root.lng.number ===== 1L) shouldBe false
      (root.lng.number ===== 1) shouldBe false
    }
    "work for doubles" in {
      (root.dbl.number ===== root.dbl.number) shouldBe true
      (root.lng.number ===== root.dbl.number) shouldBe false
      (root.dbl.number ===== Double.MaxValue) shouldBe true
      (root.dbl.number ===== Double.MinValue) shouldBe false
      (root.dbl.number ===== 1.23) shouldBe false
    }
  }
}

object RichOptionalTest {

  case class Child(name: String, age: Int)
  case class SomeData(value: Int = 123,
                      reference: Int = 123,
                      dbl: Double = Double.MaxValue,
                      lng: Long = Long.MaxValue,
                      id: String = "foo",
                      children: List[Child] = List(Child("first", 8), Child("second", 3)))
}
