package pipelines.expressions

import scala.util.{Success, Try}

/**
  * A really dumb, lazy cache of expressions
  */
class Cache[V](create: String => Try[V], default: V) {
  private object Lock

  private var predicateByRule = Map[String, V]()

  private def createUnsafe(expression: String): Try[V] = {
    create(expression).map { value =>
      predicateByRule = predicateByRule.updated(expression, value)
      value
    }
  }

  def apply(expression: String): Try[V] = {
    if (Option(expression).map(_.trim).filter(_.nonEmpty).isDefined) {
      Lock.synchronized {
        predicateByRule.get(expression) match {
          case None         => createUnsafe(expression)
          case Some(cached) => Success(cached)
        }
      }
    } else {
      Success(default)
    }
  }
}
