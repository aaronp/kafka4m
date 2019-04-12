package pipelines.eval

import java.nio.file.Path

import eie.io._
import example._
import pipelines.DynamicAvroRecord
import org.scalatest.{Matchers, WordSpec}

import scala.language.dynamics
import scala.util.{Properties, Success, Try}

class AvroReaderTest extends WordSpec with Matchers {

  "AvroReader" should {

    val beispielData = for {
      id   <- Seq("a", "b", "c")
      text <- Seq(Option("text1"), None)
      day  <- tagDesWoche.values().take(3)
    } yield {
      val default = Beispiel.newBuilder().setKennzeichnen(id).setTag(day)
      text.fold(default)(default.setWort).build()
    }

    val exampleData = for {
      id   <- Seq("a", "b", "c")
      text <- Seq(Option("text1"), None)
      day  <- daysOfTheWeek.values().take(3)
    } yield {
      val default = Example.newBuilder().setId(id).setAnEnum(day)
      text.fold(default)(default.setSomeText).build()
    }

    val deBytes: Array[Byte] = {
      val bytes = beispielData.head.toByteBuffer.array()
      bytes.length should be > 0
      bytes
    }

    "read specific types" in {
      val deReader = AvroReader[Beispiel]
      beispielData.foreach { expected =>
        val actual: Try[Beispiel] = deReader.read(expected.toByteBuffer.array)
        actual shouldBe Success(expected)
      }
    }

    "read generic types" in {

      def findExampleSchema(name: String) = {
        val schemaFile: Path = {
          val root = Properties.userDir.asPath
          (root +: root.parents).map(_.resolve(s"example/src/main/avro/$name")).find(_.isFile).get
        }
        schemaFile.text

      }

      val deReader: AvroReader[DynamicAvroRecord] = AvroReader.generic(findExampleSchema("beispiel.avsc"))
//      val enReader = AvroReader.generic(findExampleSchema("example.avsc"))

      def check(readBack: DynamicAvroRecord, record: Beispiel) = {
        readBack.kennzeichnen.asString shouldBe record.getKennzeichnen

        val tag: DynamicAvroRecord.Value = readBack.getTag
        val actualEnum                   = tag.as[tagDesWoche]
        val expectedEnum                 = record.getTag
        actualEnum shouldBe expectedEnum

        readBack.tag.as[tagDesWoche] shouldBe record.getTag
        readBack.wort.asString shouldBe record.getWort
      }

      check(deReader.read(deBytes).get, beispielData.head)

      beispielData.foreach { expected =>
        val actual = deReader.read(expected.toByteBuffer.array).get
        check(actual, expected)
      }
    }
  }
}
