package pipelines.data

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Represents a data source, which includes some abilities to automatically convert to required types for enrichment/consumption
  *
  */
class DataSource[A: ClassTag](val data: Observable[A], val enrichments: Seq[ModifyObservable] = Nil) {
  type T = A

  override def toString: String = {
    val basic = s"DataSource[$tagName]"
    enrichments match {
      case Seq() => basic
      case many  => many.mkString(s"${basic} {", " --> ", "}")
    }
  }
  def tag: ClassTag[A] = implicitly[ClassTag[A]]
  def tagName: String  = asId(tag)

  /** Enrichments don't modify the types (MapType does that), but not all enrichments can work on *any* type.
    *
    * For ones which only work for specific types (e.g. persisting bytes to disk or applying some fixed enrichment param) see (TypedEnrich).
    *
    * For others which work on all types (e.g. just rate limit a stream) see [[ModifyObservableAny]]
    *
    * @param enrichment
    * @param sched
    * @return
    */
  final def enrich(enrichment: ModifyObservable)(implicit sched: Scheduler): Option[DataSource[_]] = {
//    enrichment match {
//      case any: ModifyObservableAny        => enrich(any)
//      case typed: ModifyObservableTyped[A] => enrichTyped(typed)
//    }
    ???
  }
  final def enrichTyped(typed: ModifyObservableTyped[A])(implicit sched: Scheduler): DataSource[A] = {
    new DataSource[A](typed.modify(data), enrichments :+ typed)
  }
  final def enrich(enrichment: ModifyObservableAny)(implicit sched: Scheduler): DataSource[A] = {
    new DataSource[A](enrichment.modify(data), enrichments :+ enrichment)
  }

  final def connect(consumer: DataSink[A])(implicit sched: Scheduler): Cancelable = {
    data.subscribe(consumer.sink)
  }
}
object DataSource {

  class PushSource[A: ClassTag](val input: Observer[A], obs: Observable[A]) extends DataSource[A](obs) {
    override def tag: ClassTag[A]   = implicitly[ClassTag[A]]
    def push(value: A): Future[Ack] = input.onNext(value)

  }

  def push[A: ClassTag](implicit sched: Scheduler): PushSource[A] = {
    val (input: Observer[A], output: Observable[A]) = Pipe.publish[A].multicast
    new PushSource[A](input, output)
  }

  def apply[A: ClassTag](obs: Observable[A]): DataSource[A] = new DataSource[A](obs)
}
