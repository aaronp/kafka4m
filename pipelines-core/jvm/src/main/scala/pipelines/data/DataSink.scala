package pipelines.data

import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.reactive.Observer

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class DataSink[A: ClassTag] {
  def tag: ClassTag[A]          = implicitly[ClassTag[A]]
  def name: String              = asId(tag)
  override def toString: String = name
  def sink: Observer[A]
}
object DataSink {

  def collect[A: ClassTag](): Collect[A] = {
    new Collect[A]()
  }

  def apply[A: ClassTag](obs: Observer[A], name: String = null): DataSink[A] = {
    val inputName = Option(name)
    new DataSink[A] {
      override def sink: Observer[A] = obs
      override val name              = inputName.getOrElse(super.name)
    }
  }

  case class Collect[A: ClassTag]() extends DataSink[A] {
    private var ack: Ack = Continue
    private val buffer   = new ListBuffer[A]()
    def clear()          = buffer.clear()
    def toList()         = buffer.toList
    def cancel() = {
      ack = Stop
    }
    override def sink: Observer[A] = new Observer[A] {
      override def onNext(next: A): Future[Ack] = {
        buffer += next
        ack
      }
      override def onError(ex: Throwable): Unit = {}
      override def onComplete(): Unit           = {}
    }
  }
}
