package pipelines.data

import monix.execution.atomic.AtomicAny
import monix.reactive.Observable
import pipelines.core.{DataType, Rate}

import scala.reflect.ClassTag

class DataSourceRateLimitAll[A: ClassTag] private (underlying: DataSource[A], val limitRef: AtomicAny[Rate]) extends DataSource[A] {
  override val tag = implicitly[ClassTag[A]]
  override def data: Observable[A] = {
    underlying.data.bufferTimed(limitRef.get.per).map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
  }

  override def sourceType: DataType = underlying.sourceType
}
object DataSourceRateLimitAll {
  def apply[A: ClassTag](underlying: DataSource[A], initialRate: Rate): DataSourceRateLimitAll[A] = {
    val limitRef = AtomicAny(initialRate)
    new DataSourceRateLimitAll[A](underlying, limitRef)
  }
}
