package pipelines.expresssions.ast

sealed trait Op

case object Plus extends Op

case object Minus extends Op

case object Times extends Op

case object DividedBy extends Op

case object Modulo extends Op
