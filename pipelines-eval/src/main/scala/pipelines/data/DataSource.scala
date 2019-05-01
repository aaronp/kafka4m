package pipelines.data

import cats.Functor
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, Observer}

import scala.reflect.ClassTag

/**
  * Represents something which can be used as a data source
  *
  * @tparam A
  */
trait DataSource[A] {

  type T = A

  /** connect this source with a sink
    *
    * @param sink
    * @param sched
    * @return a cancelable
    */
  def connect(sink: DataSink[_], sched: Scheduler): Either[ClassCastException, Cancelable] = {
    try {
      val cancelable: Cancelable = data.subscribe(sink.observer.asInstanceOf[Observer[A]])(sched)
      Right(cancelable)
    } catch {
      case exp: ClassCastException => Left(exp)
    }
  }

  def tag: ClassTag[A]

  def sourceType: DataType

  def data: Observable[A]

}

object DataSource {

  def apply[A: ClassTag](obs: Observable[A], `type`: DataType): DataSource[A] = {
    new DataSource[A] {
      override val tag                 = implicitly[ClassTag[A]]
      override val sourceType          = `type`
      override val data: Observable[A] = obs
    }
  }
}
