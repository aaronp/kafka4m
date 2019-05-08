package pipelines.rest.socket

import pipelines.core.CreateSourceRequest
import pipelines.data.{DataRegistry, DataRegistryResponse}

trait SourceFactory {
  def create(newSource: CreateSourceRequest, id: Option[String]): DataRegistryResponse
}

object SourceFactory {
  def apply(registry: DataRegistry) = new SourceFactory {
    override def create(newSource: CreateSourceRequest, id: Option[String]): DataRegistryResponse = {
      ???
    }
  }
}
