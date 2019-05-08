package pipelines.data

import monix.execution.{Cancelable, Scheduler}
import monix.execution.schedulers.SchedulerService
import pipelines.core.{Rate, StreamStrategy}

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
final case class DataRegistry(sources: SourceRegistry, sinks: SinkRegistry, defaultIOScheduler: Scheduler, lookup: ModifyObservableResolver) extends AutoCloseable {

  implicit def execContext = defaultIOScheduler
  import ModifyObservableRequest._

  def addStatistics(source: String, newSource: String, verbose: Boolean): _root_.pipelines.data.DataRegistryResponse = ???

  def update(request: DataRegistryRequest): DataRegistryResponse = {
    request match {
      case Connect(source, sink)                                          => connect(source, sink)
      case ModifySourceRequest(source, newSource, AddStatistics(verbose)) => addStatistics(source, newSource, verbose)
      case ModifySourceRequest(source, newSource, enrichment) =>
        lookup.resolve(enrichment) match {
          case None    => SourceErrorResponse(source, s"Enrichment couldn't be found for $enrichment")
          case Some(e) => enrichSource(source, newSource, e)
        }
      case UpdateSourceRequest(sourceKey, enrichment) =>
        lookup.resolve(enrichment) match {
          case None => SourceErrorResponse(sourceKey, s"Enrichment couldn't be found for $enrichment")
          case Some(e) =>
            withSource(sourceKey) { source =>
              lookup.update(e, source) match {
                case None =>
                  SourceErrorResponse(sourceKey, s"Updating $e had no effect")
                case Some(_) =>
                  SourceUpdatedResponse(sourceKey, s"Updated $e")
              }
            }
        }
    }
  }

  def filterSources(sourceKey: String, newSourceKey: String, expression: String): DataRegistryResponse = {
    update(ModifySourceRequest(sourceKey, newSourceKey, ModifyObservableRequest.Filter(expression)))
  }

  def rateLimitSources(sourceKey: String, newSourceKey: String, rate: Rate, strategy: StreamStrategy): DataRegistryResponse = {
    update(ModifySourceRequest(sourceKey, newSourceKey, ModifyObservableRequest.RateLimit(rate, strategy)))
  }

  def enrichSource(sourceKey: String, newSourceKey: String, enrich: ModifyObservable): DataRegistryResponse = {
    withSource(sourceKey) { source =>
      source.enrich(enrich) match {
        case None =>
          SourceErrorResponse(sourceKey, s"Couldn't convert $sourceKey of type ${source.tagName}")
        case Some(newSource) =>
          if (sources.register(newSourceKey, newSource)) {
            SourceCreatedResponse(newSourceKey, newSource.tagName)
          } else {
            SourceAlreadyExistsResponse(newSourceKey)
          }
      }
    }
  }

  /** connect a source to a sink
    *
    * @param sourceKey
    * @param sinkKey
    */
  def connect(sourceKey: String, sinkKey: String): DataRegistryResponse = withSource(sourceKey) { source: DataSource[_] =>
    sinks.get(sinkKey) match {
      case None                    => SinkNotFoundResponse(sinkKey)
      case Some(sink: DataSink[_]) =>
        // we can expose an explicit 'runOn' source which we could match here
        val cancelable: Cancelable = ??? // source.connect(sink)
        sinks.notifyConnected(sourceKey, sinkKey, cancelable)
        ConnectResponse(sourceKey, sinkKey)
    }
  }

  private def withSource(sourceKey: String)(thunk: DataSource[_] => DataRegistryResponse): DataRegistryResponse = {
    sources.get(sourceKey) match {
      case None         => SourceNotFoundResponse(sourceKey)
      case Some(source) => thunk(source)
    }
  }

  override def close(): Unit = {
    defaultIOScheduler match {
      case ss: SchedulerService     => ss.shutdown()
      case closeable: AutoCloseable => closeable.close()
      case _                        =>
    }
  }
}

object DataRegistry {

  def apply(ioScheduler: Scheduler, resolver: ModifyObservableResolver = ModifyObservableResolver()): DataRegistry = {
    new DataRegistry(SourceRegistry(ioScheduler), SinkRegistry(ioScheduler), ioScheduler, resolver)
  }
}
