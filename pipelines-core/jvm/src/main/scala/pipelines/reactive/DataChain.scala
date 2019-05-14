package pipelines.reactive

import monix.reactive.Observable
import pipelines.reactive.DataChain.DataSourceStep

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
  * Represents an addressable space of computation steps (data sources).
  *
  * Each 'step' is a data source -- either the root source, or a transformation/step based on previous steps.
  *
  * @param steps
  */
case class DataChain private (steps: Seq[DataChain.DataSourceStep]) {
  require(steps.size > 0)
  require(steps.count(_.parent == None) == 1, s"A ${getClass} should have a single root")

  def get(index: Int): Option[DataChain.DataSourceStep] = {
    steps.lift(index)
  }

  def keys: Seq[Int]                                  = steps.map(_.key)
  def names: Seq[String]                              = steps.map(_.name).distinct.sorted
  def stepsForName(name: String): Seq[DataSourceStep] = steps.filter(_.name == name)

  /** @return the highest key
    */
  def maxKey: Int    = keys.max
  def sourceKey: Int = keys.min

  /** Update the chain by adding a new transformation
    *
    * @param to the id of the data source or transform to which this transform should be applied
    * @param name a descriptive name to what this transform does
    * @param transform the transform to add
    * @return either an error message or an updated DataChain together with the ID of the newly added chain
    */
  def addTransform(to: Int, name: String, transform: Transform): Either[String, (DataChain, Int)] = {
    get(to) match {
      case None => Left(s"No step with id '$to' exists, and so unfortunatley we won't be able to add ransform '${name}'")
      case Some(parentStep) =>
        val dataInput = parentStep.dataSource

        transform.applyTo(dataInput) match {
          case Some(nextSource) =>
            val nextKey = maxKey + 1
            transform.outputFor(nextSource.contentType) match {
              case Some(nextResult) =>
                val desc     = s"$name ($nextResult)"
                val nextStep = DataSourceStep(nextKey, dataInput.contentType, nextResult, desc, nextSource, Option(parentStep.key))
                Right(copy(steps = steps :+ nextStep) -> nextKey)
              case None => Left(s"Couldn't apply $name as ${transform}.outputFor(${nextSource.contentType}) was undefined, despite it apparently applying to $dataInput")
            }

          case None => Left(s"Transform '${name}' couldn't be applied to ${dataInput.contentType}")
        }
    }
  }

  /** @param key the step key
    * @param onConnect
    * @return the result of 'onConnect' if the source exists for 'key'
    */
  def connect[A](key: Int)(onConnect: Observable[_] => A): Try[A] = {
    get(key).fold(Failure(new IllegalArgumentException(s"No source found for $key")): Try[A]) { step =>
      step.connect(onConnect)
    }
  }
}

object DataChain {
  case class DataSourceStep(key: Int, input: ContentType, output: ContentType, name: String, desc: String, dataSource: Data, parent: Option[Int]) {
    def connect[A](onConnect: Observable[_] => A): Try[A] = {
      try {
        val result = onConnect(dataSource.asObservable)
        Success(result)
      } catch {
        case NonFatal(e) =>
          Failure(new Exception(s"Error connecting to '$name' (key $key):$e", e))
      }
    }
  }
  object DataSourceStep {

    def apply(key: Int, input: ContentType, output: ContentType, name: String, dataSource: Data, parent: Option[Int] = None) = {
      new DataSourceStep(key, input, output, name, name, dataSource, parent)
    }
  }

  /**
    * Create a ConnectedPipeline from the given transformations (compute chain)
    *
    * @param source
    * @param transforms
    * @return
    */
  def apply(source: Data, transforms: Seq[(String, Transform)]): Either[String, DataChain] = {
    val root = DataSourceStep(0, source.contentType, source.contentType, s"source of ${source.contentType}", source, None)
    val stepsEither = transforms.zipWithIndex.foldLeft(Right(Seq(root)): Either[String, Seq[DataSourceStep]]) {
      case (Left(err), _) => Left(err)
      case (Right(steps @ (previous +: _)), ((name, transform), index)) =>
        val key       = index + 1
        val dataInput = previous.dataSource
        transform.applyTo(dataInput) match {
          case Some(nextSource) =>
            transform.outputFor(nextSource.contentType) match {
              case None =>
                Left(s"Couldn't apply $name as ${transform}.outputFor(${nextSource.contentType}) was undefined, despite it apparently applying to $dataInput")
              case Some(nextResult) =>
                val desc     = s"transforms ${dataInput.contentType} to $nextResult)"
                val nextStep = DataSourceStep(key, dataInput.contentType, nextResult, name, desc, nextSource, Option(previous.key))
                Right(nextStep +: steps)
            }
          case None => Left(s"Transform '${name}' couldn't be applied to ${dataInput.contentType}")
        }
    }

    stepsEither.right.map(steps => new DataChain(steps.reverse))
  }

}
