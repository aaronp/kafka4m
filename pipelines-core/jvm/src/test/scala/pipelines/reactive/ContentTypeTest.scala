package pipelines.reactive

import pipelines.data.BaseCoreTest

import scala.util.Try

class ContentTypeTest extends BaseCoreTest{

  "ContentType.of" should {
    "return the parameterized types" in {
      val ct = ContentType.of[Array[Byte]]
      ct.toString shouldBe ("Array[Byte]")
      ct shouldBe ClassType("Array", Seq(ClassType("Byte")))

      ContentType.of[List[Try[String]]] shouldBe ClassType("List", Seq(ClassType("Try", Seq(ClassType("String")))))
    }
  }
}
