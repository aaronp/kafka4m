package pipelines.data

import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}

import scala.concurrent.Future

/**
  * An in-memory map of data sinks
  */
class SinkRegistry private (listeners: Observer[SinkRegistryEvent], val events: Observable[SinkRegistryEvent]) {
  def notifyConnected(sourceKey: String, sinkKey: String, cancelable: Cancelable): Future[Ack] = {
    listeners.onNext(DataSinkConnected(sourceKey, sinkKey, cancelable))
  }

  private val sinkByName = collection.mutable.HashMap[String, DataSink[_]]()

  private object Lock

  def keys(): collection.Set[String] = Lock.synchronized {
    sinkByName.keySet
  }
  def get(key: String): Option[DataSink[_]] = Lock.synchronized {
    sinkByName.get(key)
  }

  def remove(key: String): Boolean = Lock.synchronized {
    sinkByName.remove(key) match {
      case Some(dataSource) =>
        listeners.onNext(DataSinkRemoved(key, dataSource))
        true
      case None =>
        false
    }
  }
  def register(key: String, source: DataSink[_]) = Lock.synchronized {
    sinkByName.get(key) match {
      case Some(_) => false
      case None =>
        sinkByName.update(key, source)
        listeners.onNext(DataSinkRegistered(key, source))
        true
    }
  }
}

object SinkRegistry {
  def apply(implicit sche: Scheduler): SinkRegistry = {
    val (listenerFeed, listeners) = Pipe.publish[SinkRegistryEvent].multicast
    new SinkRegistry(listenerFeed, listeners)
  }
}
