package pipelines.expressions

import io.circe.Json
import io.circe.optics.JsonPath
import monocle.Optional

trait LowPriorityOptionalImplicits {
  implicit def asRichOptional[A](value: Optional[Json, A]) = new RichOptional[A](value)
  implicit def asRichJPath(path: JsonPath)                 = new RichJsonPath(path)
}
