package pipelines.data

import pipelines.core.DataType

import scala.util.Try

/**
  * Represents something which can create type filters (predicates) from an expression
  */
trait FilterAdapter {

  /**
    * This weird signature is so that invalid expressions don't end up get retried for all 'orElse' cases.
    *
    * E.g. we could try and parse it as a json expression, which could be invalid, so a Some(Failure(...)) makes sense.
    *
    * If an invalid expression was just a None then we could end up retrying to apply it as e.g. an avro filter, or protobuf...
    *
    * @param sourceType
    * @param expression
    * @tparam A
    * @return
    */
  def createFilter[A](sourceType: DataType, expression: String): Option[Try[A => Boolean]]

  final def orElse(other: FilterAdapter) = {
    val parent = this
    new FilterAdapter {
      override def createFilter[A](sourceType: DataType, expression: String): Option[Try[A => Boolean]] = {
        parent.createFilter[A](sourceType, expression).orElse {
          other.createFilter[A](sourceType, expression)
        }
      }
    }
  }
}
