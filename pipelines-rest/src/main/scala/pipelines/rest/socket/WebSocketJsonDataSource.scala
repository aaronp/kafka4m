package pipelines.rest.socket

import io.circe.Json
import monix.reactive.Observable
import pipelines.core.{DataType, JsonRecord}
import pipelines.data.DataSource

import scala.reflect.ClassTag

/**
* A data source which accepts input from a web socket
  */
class WebSocketJsonDataSource extends DataSource[Json] {
  override def tag: ClassTag[Json] = {
    ClassTag[Json](classOf[Json])
  }

  override def sourceType: DataType = JsonRecord

  override def data: Observable[Json] = ???
}
