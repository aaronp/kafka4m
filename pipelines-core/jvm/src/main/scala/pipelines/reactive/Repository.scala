package pipelines.reactive

import monix.execution.Scheduler

import scala.util.{Failure, Success, Try}

/**
  * Represents a repository of sources, sinks, and data transformations which can be used to construct
  * data pipelines by reference
  */
case class Repository(sourcesByName: Map[String, NewSource],                    //
                      sinksByName: Map[String, Kitchen],                                   //
                      configurableTransformsByName: Map[String, ConfigurableTransform[_]], //
                      transformsByName: Map[String, Transform])(implicit scheduler: Scheduler) {

  def withTransform(name: String, transform: Transform): Repository = {
    copy(transformsByName = transformsByName.updated(name, transform))
  }
  def withConfigurableTransform(name: String, transform: ConfigurableTransform[_]): Repository = {
    copy(configurableTransformsByName = configurableTransformsByName.updated(name, transform))
  }
  def withSink(name: String, sink: Kitchen): Repository = {
    copy(sinksByName = sinksByName.updated(name, sink))
  }

  def resolveTransforms(transforms: Seq[DataTransform]): Seq[(String, Try[Transform])] = {
    transforms.map {
      case DataTransform(id, configOpt) =>
        val transformResult: Try[Transform] = {
          val vanillaTranformOpt = transformsByName.get(id).map {
            case transform => Success(transform)
          }

          val resolvedOpt: Option[Try[Transform]] = vanillaTranformOpt.orElse {
            configurableTransformsByName.get(id).map { configurable: ConfigurableTransform[_] =>
              configOpt match {
                case None       => configurable.defaultTransform
                case Some(conf) => configurable.updateFromJson(conf)
              }
            }
          }

          resolvedOpt.getOrElse(Failure(new IllegalArgumentException(s"Couldn't find any transform for '${id}'")))
        }
        id -> transformResult
    }
  }

  /** A convenience method to create a new chain
    *
    * @param dataSource
    * @param transforms
    * @return either an error or a new chain
    */
  def createChain(dataSource: String, firstTransform: DataTransform, transforms: DataTransform*): Either[String, DataChain] = {
    createChain(CreateChainRequest(dataSource, firstTransform +: transforms.toSeq))
  }

  /** A convenience method to create a new chain
    *
    * @param dataSource
    * @param transforms
    * @return either an error or a new chain
    */
  def createChain(dataSource: String, transforms: String*): Either[String, DataChain] = {
    transforms.map(name => DataTransform(name)) match {
      case Seq()           => createChain(CreateChainRequest(dataSource, Nil))
      case head +: theRest => createChain(dataSource, head, theRest: _*)
    }
  }

  /**
    * Create a new [[DataChain]] using the sources/transforms in this repo
    *
    * @param request the create request
    * @return either an error message or a new DataChain
    */
  def createChain(request: CreateChainRequest): Either[String, DataChain] = {
    val sourceTry: Try[Data] = sourcesByName.get(request.dataSource) match {
      case Some(data) => Success(data(scheduler))
      case None       => Failure(new IllegalArgumentException(s"Couldn't find source for '${request.dataSource}'"))
    }

    val transById: Seq[(String, Try[Transform])] = resolveTransforms(request.transforms)

    sourceTry match {
      case Success(source) =>
        val errors: Seq[(String, Throwable)] = transById.collect {
          case (key, Failure(err)) => (key, err)
        }

        errors match {
          case Seq() =>
            val transforms = (transById: @unchecked).map {
              case (id, Success(t)) => (id, t)
            }
            DataChain(source, transforms)
          case (key, err) +: theRest =>
            val messages = (err.getMessage +: theRest.map(_._2.getMessage)).distinct
            val keys     = (key +: theRest.map(_._1)).distinct
            Left(s"Found ${messages.size} error(s) creating a pipeline for transformation(s) ${keys.mkString("'", "', '", "'")} : ${messages.mkString("; ")}")
        }
      case Failure(err) => Left(err.getMessage)
    }
  }

  private lazy val sinks = sinksByName.keySet.map(ListedSink.apply).toList.sortBy(_.name)

  def listSinks(request: ListSinkRequest) = {
    ListSinkResponse(sinks)
  }

  private lazy val allTransforms: List[ListedTransformation] = transformsByName.keySet
    .map { key =>
      ListedTransformation(key, None)
    }
    .toList
    .sortBy(_.name)
  def listTransforms(request: ListTransformationRequest) = {
    val found = request.inputContentType.fold(allTransforms) { accepts =>
      transformsByName
        .flatMap {
          case (key, transform) =>
            transform.outputFor(accepts).map { out =>
              ListedTransformation(key, Option(out))
            }
        }
        .toList
        .sortBy(_.name)
    }
    ListTransformationResponse(found)
  }

  private lazy val allSources = sourcesByName.map {
    case (key, d8a) => ListedDataSource(key, Option(d8a(scheduler).contentType))
  }

  def listSources(request: ListRepoSourcesRequest) = {
    val sources = request.ofType.fold(allSources) { accepts =>
      sourcesByName.collect {
        case (key, d8a) if d8a(scheduler).data(accepts).isDefined => ListedDataSource(key, Option(d8a(scheduler).contentType))
      }
    }
    ListRepoSourcesResponse(sources.toList.sortBy(_.name))
  }
}

object Repository {

  def apply(sourcesByName: (String, NewSource)*)(implicit scheduler: Scheduler): Repository = {
    val sourceMap = sourcesByName.toMap.ensuring(_.size == sourcesByName.size)
    val sinkMap   = Map("sink" -> Kitchen())
    apply(sourceMap, sinkMap, Map.empty[String, ConfigurableTransform[_]], Map.empty[String, Transform])
  }

  def apply(firstSource: (String, Data), sourcesByName: (String, Data)*)(implicit scheduler: Scheduler): Repository = {
    val newSources = (firstSource +: sourcesByName).map {
      case (name, d8a) => (name, NewSource(d8a))
    }
    apply(newSources: _*)
  }

}
