package pipelines.data

import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import pipelines.stream.{ListSourceResponse, RegisteredSource}

/**
  * An in-memory map of data sources
  */
class SourceRegistry private (listeners: Observer[SourceRegistryEvent], val events: Observable[SourceRegistryEvent]) {

  private val sourcesByName = collection.mutable.HashMap[String, DataSource[_]]()

  private object Lock

  def keys(): collection.Set[String] = Lock.synchronized {
    sourcesByName.keySet
  }
  def list(): ListSourceResponse = Lock.synchronized {
    val all = sourcesByName.map {
      case (key, source) =>
        RegisteredSource(key, source.sourceType.name)
    }
    ListSourceResponse(all.toList.sortBy(_.name))
  }

  def remove(key: String): Boolean = Lock.synchronized {
    sourcesByName.remove(key) match {
      case Some(dataSource) =>
        listeners.onNext(DataSourceRemoved(key, dataSource))
        true
      case None =>
        false
    }
  }
  def get(key: String): Option[DataSource[_]] = Lock.synchronized {
    sourcesByName.get(key)
  }

  def register(key: String, source: DataSource[_]): Boolean = Lock.synchronized {
    sourcesByName.get(key) match {
      case Some(_) => false
      case None =>
        sourcesByName.update(key, source)
        listeners.onNext(DataSourceRegistered(key, source))
        true
    }
  }
}

object SourceRegistry {
  def apply(implicit scheduler: Scheduler): SourceRegistry = {
    val (listenerFeed, listeners) = Pipe.publish[SourceRegistryEvent].multicast
    new SourceRegistry(listenerFeed, listeners)
  }
}
