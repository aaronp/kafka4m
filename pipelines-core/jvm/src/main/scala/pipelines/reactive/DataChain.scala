package pipelines.reactive

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

import monix.reactive.Observable

import scala.util.control.NonFatal

/**
  * Represents an addressable space of computation steps (data sources).
  *
  * Each 'step' is a data source -- either the root source, or a transformation/step based on previous steps.
  *
  * @param steps
  * @param created
  */
case class DataChain private (steps: Seq[DataChain.DataSourceStep], created: ZonedDateTime = ZonedDateTime.now) {
  require(steps.size > 0)
  require(steps.count(_.parent == None) == 1, s"A ${getClass} should have a single root")

  def get(index: Int): Option[DataChain.DataSourceStep] = {
    steps.lift(index)
  }

  def keys: Seq[Int] = steps.map(_.key)

  def connect(index: Int, onConnect: Observable[_] => Unit): Boolean = {
    get(index).fold(false) { step =>
      step.connect(onConnect)
      true
    }
  }
}

object DataChain {
  case class DataSourceStep(key: Int, desc: String, dataSource: Data, parent: Option[Int]) {
    def connect(onConnect: Observable[_] => Unit): Unit = {
      try {
        onConnect(dataSource.asObservable)
      } catch {
        case NonFatal(e) =>
          throw new IllegalStateException(s"Error trying to connect to '$desc' ($dataSource)", e)
      }
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
    val uniqueNameCounterByName: Map[String, AtomicInteger] = transforms.map(_._1).groupBy(identity).mapValues {
      case values if values.size == 1 => new AtomicInteger(-1)
      case _                          => new AtomicInteger(1)
    }

    val root = DataSourceStep(0, s"source of ${source.contentType}", source, None)
    val stepsEither = transforms.zipWithIndex.foldLeft(Right(Seq(root)): Either[String, Seq[DataSourceStep]]) {
      case (Left(err), _) => Left(err)
      case (Right(steps @ (previous +: _)), ((name, transform), index)) =>
        val key       = index + 1
        val dataInput = previous.dataSource
        transform.applyTo(dataInput) match {
          case Some(nextSource) =>
            val nextResult = transform.outputFor(nextSource.contentType)
            val desc = uniqueNameCounterByName(name).incrementAndGet match {
              case 0 => s"$name ($nextResult)"
              case n => s"$name-$n ($nextResult)"
            }
            val nextStep = DataSourceStep(key, desc, nextSource, Option(previous.key))
            Right(nextStep +: steps)
          case None => Left(s"Transform '${name}' couldn't be applied to ${dataInput.contentType}")
        }
    }

    stepsEither.right.map(steps => new DataChain(steps.reverse))
  }

}
