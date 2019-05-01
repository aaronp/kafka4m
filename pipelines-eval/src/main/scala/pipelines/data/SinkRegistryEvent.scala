package pipelines.data

import monix.execution.Cancelable

sealed trait SinkRegistryEvent
case class DataSinkRegistered[A](name: String, dataSource: DataSink[A])    extends SinkRegistryEvent
case class DataSinkRemoved[A](name: String, dataSource: DataSink[A])       extends SinkRegistryEvent
case class DataSinkConnected[A](sourceKey: String, sinkKey : String, cancelable: Cancelable) extends SinkRegistryEvent
