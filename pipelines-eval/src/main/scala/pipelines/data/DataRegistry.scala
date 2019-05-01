package pipelines.data

import monix.execution.Scheduler

import scala.util.{Failure, Success}

/**
  * A generic means of connecting sources and sinks.
  *
  * Some thinks we might want to generically be able to:
  * $ connect generic sources and sinks
  * $ create new filtered sources from existing ones
  * $ add rate-limiting sources
  * $ create transformations so that we can e.g. unmarhsall bytes to Avro records, or avro records into Json, or protobufs to files
  * $ persist sources (e.g. convert a websocket source to one which goes via disk, and thus exposing a new data-source from disk)
  * $ write down statistics from a source (rate, total bytes)
  * $ create pause-able sources
  *
  * @param sources
  * @param sinks
  */
case class DataRegistry(sources: SourceRegistry, sinks: SinkRegistry, defaultScheduler: Scheduler) {

  def update(request: DataRegistryRequest)(implicit adapterEvidence: TypeAdapter.Aux, filter: FilterAdapter): DataRegistryResponse = {
    request match {
      case Connect(source, sink)                         => connect(source, sink)
      case FilterSourceRequest(source, newSource, expr)  => filterSources(source, newSource, expr)
      case UpdateFilterRequest(source, expr)             => ???
      case ChangeTypeRequest(source, newSource, newType) => changeType(source, newSource, newType)
    }
  }

  def filterSources(sourceKey: String, newSourceKey: String, expr: String)(implicit createFilter: FilterAdapter): DataRegistryResponse = withSource(sourceKey) { source =>
    createFilter.createFilter(source.sourceType, expr) match {
      case Some(Success(filter)) =>
        val (updateFilter, newSource) = DataSourceFiltered.from(source)
        updateFilter := filter

        if (sources.register(newSourceKey, newSource)) {
          SourceCreatedResponse(newSourceKey, newSource.sourceType)
        } else {
          SourceAlreadyExistsResponse(newSourceKey)
        }
      case Some(Failure(error)) => ErrorCreatingSource(newSourceKey, s"Couldn't parse ${source.sourceType} expression >${expr}< : $error")
      case None                 => ErrorCreatingSource(newSourceKey, s"Couldn't find a filter adapter for ${source.sourceType} for expression >${expr}<")
    }
  }

  def changeType(sourceKey: String, newSourceKey: String, newType: DataType)(implicit adapterEvidence: TypeAdapter.Aux): DataRegistryResponse = {
    withSource(sourceKey) { source =>
      adapterEvidence.map[source.T](source.sourceType, newType) match {
        case Some(typeAdapter: TypeAdapter[source.T]) =>
          val newSource: DataSourceMapped[source.T, typeAdapter.T] = ??? //DataSourceMapped[source.T, typeAdapter.T](source, typeAdapter.sourceType, typeAdapter.map)
          if (sources.register(newSourceKey, newSource)) {
            SourceCreatedResponse(newSourceKey, newType)
          } else {
            SourceAlreadyExistsResponse(newSourceKey)
          }
        case None =>
          UnsupportedTypeMappingResponse(sourceKey, source.sourceType, newType)
      }
    }
  }

  /**
    *
    * @param sourceKey
    * @param sinkKey
    */
  def connect(sourceKey: String, sinkKey: String): DataRegistryResponse = withSource(sourceKey) { source =>
    sinks.get(sinkKey) match {
      case None       => SinkNotFoundResponse(sinkKey)
      case Some(sink) =>
        // we can expose an explicit 'runOn' source which we could match here
        source.connect(sink, defaultScheduler) match {
          case Right(cancelable) =>
            sinks.notifyConnected(sourceKey, sinkKey, cancelable)
            ConnectResponse(sourceKey, sinkKey)
          case Left(_) =>
            SourceSinkMismatchResponse(sourceKey, sinkKey, source.sourceType, sink.sinkType)
        }
    }
  }

  private def withSource(sourceKey: String)(thunk: DataSource[_] => DataRegistryResponse): DataRegistryResponse = {
    sources.get(sourceKey) match {
      case None         => SourceNotFoundResponse(sourceKey)
      case Some(source) => thunk(source)
    }
  }
}

object DataRegistry {
  def apply(implicit scheduler: Scheduler): DataRegistry = {
    new DataRegistry(SourceRegistry(scheduler), SinkRegistry(scheduler), scheduler)
  }
}
