package esa.json
import io.circe.{Json, parser}
import org.scalatest.{Matchers, WordSpec}

class JsonUtilsTest extends WordSpec with Matchers {

  "JsonUtil.flatten" should {
    "return all the paths for some json" in {

      val jsonString = """{
        "aString" : "text",
        "anInt" : 7,
        "aList" : [1,2,3],
        "aBoolean" : true,
        "aNull" : null,
        "nestedObj" : {
          "deep" : {
            "id" : 1,
            "b" : false
          },
          "array" : [
            {
              "id" : 4
            },
            {
              "id" : 2,
              "foo" : "bar"
            }
          ]
        }
      }"""

      val Right(input)                 = parser.parse(jsonString)
      val actual: List[(String, Json)] = JsonUtils.flatten(input)

      actual should contain allOf (
        "aString"               -> Json.fromString("text"),
        "anInt"                 -> Json.fromInt(7),
        "aList.0"               -> Json.fromInt(1),
        "aList.1"               -> Json.fromInt(2),
        "aList.2"               -> Json.fromInt(3),
        "aBoolean"              -> Json.fromBoolean(true),
        "aNull"                 -> Json.Null,
        "nestedObj.deep.id"     -> Json.fromInt(1),
        "nestedObj.deep.b"      -> Json.fromBoolean(false),
        "nestedObj.array.0.id"  -> Json.fromInt(4),
        "nestedObj.array.1.id"  -> Json.fromInt(2),
        "nestedObj.array.1.foo" -> Json.fromString("bar")
      )
    }
  }
}
