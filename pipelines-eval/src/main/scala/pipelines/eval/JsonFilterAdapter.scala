package pipelines.eval

import io.circe.Json
import pipelines.core.{DataType, JsonRecord}
import pipelines.data.FilterAdapter
import pipelines.expressions.{Cache, JsonExpressions}

import scala.util.Try

object JsonFilterAdapter {
  def apply(): JsonFilterAdapter = new JsonFilterAdapter()
}

class JsonFilterAdapter(computeCache: Cache[Json => Boolean] = JsonExpressions.newCache) extends FilterAdapter {
  override def createFilter[A](sourceType: DataType, expression: String): Option[Try[A => Boolean]] = {
    if (sourceType == JsonRecord) {
      val result = Option(computeCache(expression))
      result.asInstanceOf[Option[Try[A => Boolean]]]
    } else {
      None
    }
  }
}
