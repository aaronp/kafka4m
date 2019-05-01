package pipelines.data

import monix.reactive.Observable

import scala.reflect.ClassTag

class DataSourceMapped[A, B: ClassTag] private (underlying: DataSource[A], override val sourceType: DataType, transform: A => B) extends DataSource[B] {
  override val tag                 = implicitly[ClassTag[B]]
  override def data: Observable[B] = underlying.data.map(transform)
}

object DataSourceMapped {
  def apply[A, B: ClassTag](underlying: DataSource[A], sourceType: DataType, transform: A => B): DataSourceMapped[A, B] = {
    new DataSourceMapped(underlying, sourceType, transform)
  }
}
