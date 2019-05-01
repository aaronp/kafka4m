package pipelines.data

import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.reactive.Observer

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

trait DataSink[A] {
  def sinkType: DataType
  def observer: Observer[A]
}

object DataSink {
  class Collect[A](override val sinkType: DataType) extends DataSink[A] {
    private var ack: Ack = Continue
    private val buffer   = new ListBuffer[A]()
    def toList()         = buffer.toList
    def cancel() = {
      ack = Stop
    }
    override def observer: Observer[A] = new Observer[A] {
      override def onNext(next: A): Future[Ack] = {
        buffer += next
        ack
      }
      override def onError(ex: Throwable): Unit = {}
      override def onComplete(): Unit           = {}
    }
  }
  def collect[A](sinkType: DataType = AnyType): Collect[A] = new Collect[A](sinkType)

  def apply[A](obs: Observer[A], `type`: DataType): DataSink[A] = new DataSink[A] {
    override val sinkType              = `type`
    override def observer: Observer[A] = obs
  }
}
