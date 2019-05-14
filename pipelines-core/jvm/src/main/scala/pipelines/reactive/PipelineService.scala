package pipelines.reactive

import java.util.UUID

import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}

import scala.util.{Failure, Try}

trait PipelineService {

  /**
    * Create a new pipeline from this request
    *
    * @param request
    * @return either an error message or a response
    */
  def createPipeline(request: CreateChainRequest): Either[String, CreateChainResponse]

  def connectToSink(request: ConnectToSinkRequest): Try[ConnectToSinkResponse]
}

object PipelineService {

  sealed trait Event
  case class DataChainCreated(newChain: DataChain) extends Event

  def apply(repo: Repository)(implicit sched: Scheduler): PipelineService = {
    val (input: Observer[Event], output: Observable[Event]) = Pipe.replayLimited[Event](10).multicast
    new Instance(repo, input, output)
  }

  class Instance(repo: Repository, input: Observer[Event], output: Observable[Event])(implicit sched: Scheduler) extends PipelineService {

    private var chainsById = Map[String, DataChain]()
    private object Lock
    override def createPipeline(request: CreateChainRequest): Either[String, CreateChainResponse] = {
      val either: Either[String, DataChain] = repo.createChain(request)
      either.right.map { chain =>
        val id = UUID.randomUUID.toString
        Lock.synchronized {
          chainsById = chainsById.updated(id, chain)
        }
        CreateChainResponse(id)
      }
    }

    override def connectToSink(request: ConnectToSinkRequest) = {
      val found = Lock.synchronized {
        chainsById.get(request.pipelineId)
      }

      found match {
        case Some(chain) =>
          repo.sinksByName.get(request.dataSink) match {
            case Some(sink) =>
              val success = chain.connect(request.dataSourceId)(sink.connect)
              success.map { result =>
                ConnectToSinkResponse(request.dataSourceId.toString)
              }
            case None =>
              Failure(new Exception(s"Couldn't find sink '${request.dataSink}'"))
          }

        case None => Failure(new Exception(s"Couldn't find '${request.pipelineId}'"))
      }
    }
  }
}
