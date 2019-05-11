package pipelines.reactive

import java.util.UUID

import monix.execution.Scheduler

trait PipelineService {

  /**
    * We have to resolve all the sources/sinks
    *
    * @param request
    * @return
    */
  def createPipeline(request: CreateChainRequest): Either[String, CreateChainResponse]

  def connectToSink(request: ConnectToSinkRequest): Either[String, ConnectToSinkResponse]
}

object PipelineService {

  def apply(repo: Repository)(implicit sched: Scheduler) = new PipelineService {
    var chainsById = Map[String, DataChain]()
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

    override def connectToSink(request: ConnectToSinkRequest): Either[String, ConnectToSinkResponse] = {
      val found = Lock.synchronized {
        chainsById.get(request.pipelineId)
      }

      found match {
        case Some(chain) =>
          repo.sinksByName.get(request.dataSink) match {
            case Some(sink) =>
              val success = chain.connect(request.dataSourceId, obs => sink.connect(obs))
              if (success) {
                Right(ConnectToSinkResponse(request.dataSourceId.toString))
              } else {
                Left(s"Error trying to connect sink '${request.dataSink}' to ${request.dataSourceId}")
              }
            case None =>
              Left(s"Couldn't find sink '${request.dataSink}'")
          }

        case None => Left(s"Couldn't find '${request.pipelineId}'")
      }
    }
  }
}
