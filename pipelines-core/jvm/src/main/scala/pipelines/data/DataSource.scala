package pipelines.data

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}
import pipelines.core.{AnyType, DataType}

import scala.concurrent.Future
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

  class PushSource[A: ClassTag](val input: Observer[A], override val data: Observable[A]) extends DataSource[A] {
    override def tag: ClassTag[A]     = implicitly[ClassTag[A]]
    override def sourceType: DataType = AnyType(tag.runtimeClass.getName)
    def push(value: A): Future[Ack] = input.onNext(value)

  }

  def push[A: ClassTag](implicit sched: Scheduler): PushSource[A] = {
    val (input: Observer[A], output: Observable[A]) = Pipe.publish[A].multicast
    new PushSource[A](input, output)
  }

  def apply[A: ClassTag](obs: Observable[A], `type`: DataType): DataSource[A] = {
    new DataSource[A] {
      override val tag                 = implicitly[ClassTag[A]]
      override val sourceType          = `type`
      override val data: Observable[A] = obs
    }
  }
}
