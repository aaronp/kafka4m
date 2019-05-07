package pipelines.data

import monix.execution.atomic.AtomicAny
import monix.reactive.Observable
import pipelines.core.{DataType, Rate}

import scala.reflect.ClassTag

class DataSourceRateLimitLatest[A: ClassTag](underlying: DataSource[A], val limitRef: AtomicAny[Rate]) extends DataSource[A] {
  override val tag                  = implicitly[ClassTag[A]]
  override def sourceType: DataType = underlying.sourceType
  override def data: Observable[A] = {
    underlying.data.bufferTimed(limitRef.get.per).whileBusyDropEvents.map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
  }
}

object DataSourceRateLimitLatest {
  def apply[A: ClassTag](underlying: DataSource[A], initialRate: Rate): DataSourceRateLimitLatest[A] = {
    val limitRef = AtomicAny(initialRate)
    new DataSourceRateLimitLatest[A](underlying, limitRef)
  }
}
