package esa

import esa.json.JsonUtils
import esa.json.JsonUtils.JsonPath
import io.circe.Encoder

package object orientdb {

  def asProperties[T: Encoder](value: T): List[(JsonPath, String)] = {
    import io.circe.syntax._

    JsonUtils.flatten(value.asJson).map {
      case (path, value) =>
        val strValue: String = value.fold[String](
          "",
          _.toString,
          _.toString,
          identity,
          _.mkString(","),
          _.values.mkString(",")
        )
        (path, strValue)
    }
  }
}
