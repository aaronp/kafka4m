package pipelines.eval

import io.circe.Json
import pipelines.core.{DataType, JsonRecord}
import pipelines.expressions.{Cache, JsonExpressions}

import scala.util.Try

object JsonAdapter {
  def apply(): JsonAdapter = new JsonAdapter()
}

class JsonAdapter(computeCache: Cache[Json => Boolean] = JsonExpressions.newCache) extends FilterAdapter {
  override def createFilter[A](sourceType: DataType, expression: String): Option[Try[A => Boolean]] = {
    if (sourceType == JsonRecord) {
      val result = Option(computeCache(expression))
      result.asInstanceOf[Option[Try[A => Boolean]]]
    } else {
      None
    }
  }
}
