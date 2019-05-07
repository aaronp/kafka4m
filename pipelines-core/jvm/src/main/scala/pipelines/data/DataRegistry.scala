package pipelines.data

import eie.io.ToBytes
import monix.execution.Scheduler
import monix.reactive.Observable
import pipelines.core.{DataType, Enrichment, Rate, StreamStrategy}

import scala.reflect.ClassTag
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
final case class DataRegistry(sources: SourceRegistry, sinks: SinkRegistry, defaultScheduler: Scheduler) {

  def update(request: DataRegistryRequest)(implicit adapterEvidence: TypeAdapter.Aux, filter: FilterAdapter, persistDir: PersistLocation): DataRegistryResponse = {
    request match {
      case Connect(source, sink) => connect(source, sink)

      case EnrichSourceRequest(source, newSource, Enrichment.Filter(expr))              => filterSources(source, newSource, expr)
      case EnrichSourceRequest(source, newSource, Enrichment.RateLimit(rate, strategy)) => rateLimitSources(source, newSource, rate, strategy)
      case EnrichSourceRequest(source, newSource, Enrichment.MapType(newType))          => changeType(source, newSource, newType)
      case EnrichSourceRequest(source, newSource, Enrichment.PersistData(path))         => persist(source, newSource, path)
      case EnrichSourceRequest(source, newSource, Enrichment.AddStatistics(verbose))    => addStatistics(source, newSource, verbose)

      case UpdateEnrichedSourceRequest(source, Enrichment.Filter(expr))          => updateFilterSource(source, expr)
      case UpdateEnrichedSourceRequest(source, Enrichment.RateLimit(newRate, _)) => updateRateSource(source, newRate)
      case UpdateEnrichedSourceRequest(source, unsupported)                      => SourceErrorResponse(source, s"Update for $unsupported is not supported")

    }
  }

  def addStatistics(sourceKey: String, newSourceKey: String, verbose: Boolean): DataRegistryResponse = {
    withSource(sourceKey) { source =>
      val newSource = new DataSourceStats(source.data)
      if (sources.register(newSourceKey, newSource)) {
        SourceCreatedResponse(newSourceKey, newSource.sourceType)
      } else {
        SourceAlreadyExistsResponse(newSourceKey)
      }
    }
  }
  def persist(sourceKey: String, newSourceKey: String, relativePath: String)(implicit persistDir: PersistLocation): DataRegistryResponse = {
    withSource(sourceKey) { source =>
      persistDir
        .toBytes[source.T](source.sourceType)(source.tag.asInstanceOf[ClassTag[source.T]])
        .fold[DataRegistryResponse](SourceErrorResponse(newSourceKey, s"We don't know how to convert ${source.sourceType} to bytes")) { implicit toBytes: ToBytes[source.T] =>
          import eie.io._
          val dir = persistDir.dir.resolve(relativePath).mkDirs()
          def writeDown(input: Observable[source.T]): Observable[source.T] = {
            input.zipWithIndex.map {
              case (d8a, id) =>
                dir.resolve(id.toString).bytes = toBytes.bytes(d8a)
                d8a
            }

            // At the moment we DON'T expose the message index, though we could/should. That may not be such a big deal,
            // as we also intend to expose an 'Indexing' function which will essentially do this:
            //
            // Observable[T] <+> (T => String) --> Observable[(String, T)]
            //
            // so as to write down values of type T by some property which can be represented as a string
            //
            input
          }
          val src: DataSource[source.T] = source.asInstanceOf[DataSource[source.T]]
          val newSource                 = DataSourceMapped[source.T, source.T](src, source.sourceType, writeDown)
          if (sources.register(newSourceKey, newSource)) {
            SourceCreatedResponse(newSourceKey, newSource.sourceType)
          } else {
            SourceAlreadyExistsResponse(newSourceKey)
          }
        }
    }
  }

  def rateLimitSources(sourceKey: String, newSourceKey: String, rate: Rate, strategy: StreamStrategy): DataRegistryResponse = {
    withSource(sourceKey) { source =>
      val newSource = strategy match {
        case StreamStrategy.All    => DataSourceRateLimitAll(source, rate)
        case StreamStrategy.Latest => DataSourceRateLimitLatest(source, rate)
      }

      if (sources.register(newSourceKey, newSource)) {
        SourceCreatedResponse(newSourceKey, newSource.sourceType)
      } else {
        SourceAlreadyExistsResponse(newSourceKey)
      }
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
      case Some(Failure(error)) => SourceErrorResponse(newSourceKey, s"Couldn't parse ${source.sourceType} expression >${expr}< : $error")
      case None                 => SourceErrorResponse(newSourceKey, s"Couldn't find a filter adapter for ${source.sourceType} for expression >${expr}<")
    }
  }

  def updateFilterSource(sourceKey: String, expr: String)(implicit createFilter: FilterAdapter): DataRegistryResponse = withSource(sourceKey) {
    case source: DataSourceFiltered[_] =>
      createFilter.createFilter(source.sourceType, expr) match {
        case Some(Success(filter)) =>
          source.filterVar := filter
          SourceUpdatedResponse(sourceKey, s"Filter expression updated to: $expr")
        case Some(Failure(error)) => SourceErrorResponse(sourceKey, s"Couldn't parse ${source.sourceType} expression >${expr}< : $error")
        case None                 => SourceErrorResponse(sourceKey, s"Couldn't find a filter adapter for ${source.sourceType} for expression >${expr}<")
      }
    case source: DataSource[_] =>
      SourceErrorResponse(sourceKey, s"Source '${sourceKey}' for types ${source.sourceType} is not a filtered source, it is: ${source.getClass.getSimpleName}")
  }
  def updateRateSource(sourceKey: String, newRate: Rate): DataRegistryResponse = withSource(sourceKey) {
    case source: DataSourceRateLimitAll[_] =>
      val oldRate = source.limitRef.get()
      source.limitRef := newRate
      SourceUpdatedResponse(sourceKey, s"Rate updated from ${oldRate} to: $newRate")
    case source: DataSourceRateLimitLatest[_] =>
      val oldRate = source.limitRef.get()
      source.limitRef := newRate
      SourceUpdatedResponse(sourceKey, s"Rate updated from ${oldRate} to: $newRate")
    case source: DataSource[_] =>
      SourceErrorResponse(sourceKey, s"Source '${sourceKey}' for types ${source.sourceType} is not a filtered source, it is: ${source.getClass.getSimpleName}")
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

  /** connect a source to a sink
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
