package pipelines.data
import monix.execution.atomic.AtomicAny
import monix.reactive.Observable

import scala.reflect.ClassTag

class DataSourceFiltered[A: ClassTag] private (underlying: DataSource[A], filterVar: AtomicAny[A => Boolean]) extends DataSource[A] {
  override val tag = implicitly[ClassTag[A]]
  override def data: Observable[A] = {
    underlying.data.flatMap { input =>
      val predicate = filterVar.get

      if (predicate(input)) {
        Observable(input)
      } else {
        Observable.empty
      }
    }
  }

  override def sourceType: DataType = underlying.sourceType
}

object DataSourceFiltered {
  type Predicate[A] = A => Boolean

  /** @param underlying the underlying data source to filter
    * @tparam A
    * @return a callback used to push filters to this filter
    */
  def from[A: ClassTag](underlying: DataSource[A]): (AtomicAny[A => Boolean], DataSourceFiltered[A]) = {
    val filterVar = AtomicAny[A => Boolean](_ => true)
    filterVar -> new DataSourceFiltered(underlying, filterVar)
  }
}
