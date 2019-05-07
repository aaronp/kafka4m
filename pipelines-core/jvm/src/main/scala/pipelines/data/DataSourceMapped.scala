package pipelines.data

import monix.reactive.Observable
import pipelines.core.DataType

import scala.reflect.ClassTag

class DataSourceMapped[A, B: ClassTag] private (underlying: DataSource[A], override val sourceType: DataType, transform: Observable[A] => Observable[B]) extends DataSource[B] {
  override val tag                 = implicitly[ClassTag[B]]
  override def data: Observable[B] = transform(underlying.data)
}

object DataSourceMapped {
  def apply[A, B: ClassTag](underlying: DataSource[A], sourceType: DataType, transform: Observable[A] => Observable[B]): DataSourceMapped[A, B] = {
    new DataSourceMapped(underlying, sourceType, transform)
  }
}
