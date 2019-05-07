package pipelines.data

sealed trait SourceRegistryEvent
case class DataSourceRegistered[A](name: String, dataSource: DataSource[A]) extends SourceRegistryEvent
case class DataSourceRemoved[A](name: String, dataSource: DataSource[A])    extends SourceRegistryEvent
