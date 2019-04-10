package kafkaquery.eval

import example._
import org.scalatest.{Matchers, WordSpec}

class AvroReaderTest extends WordSpec with Matchers {

  "AvroReader" should {
    "read different types" in {

      val deReader = AvroReader[Beispiel]

      val input1 = Beispiel.newBuilder().setKennzeichnen("de").setTag(tagDesWoche.Mittwoch).setWort("text").build()
      val bytes = input1.toByteBuffer.array()
      deReader.read(bytes) shouldBe input1

      val enReader = AvroReader[Example]
      val input2   = Example.newBuilder().setId("en").setAnEnum(daysOfTheWeek.THURSDAY).setSomeText("foo").build()
      enReader.read(input2.toByteBuffer.array()) shouldBe input2

    }
  }
}
