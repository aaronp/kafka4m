package pipelines.eval

import pipelines.core.{AvroRecord, DataType}
import pipelines.data.FilterAdapter
import pipelines.expressions.{AvroExpressions, Cache}

import scala.util.Try

object AvroFilterAdapter {
  def apply(): JsonFilterAdapter = new JsonFilterAdapter()
}

class AvroFilterAdapter(computeCache: Cache[AvroExpressions.Predicate] = AvroExpressions.newCache) extends FilterAdapter {
  override def createFilter[A](sourceType: DataType, expression: String): Option[Try[A => Boolean]] = {
    if (sourceType == AvroRecord) {
      val result = Option(computeCache(expression))
      result.asInstanceOf[Option[Try[A => Boolean]]]
    } else {
      None
    }
  }
}
