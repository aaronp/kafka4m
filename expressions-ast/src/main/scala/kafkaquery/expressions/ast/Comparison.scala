package kafkaquery.expresssions.ast

sealed trait Comparison

case object Lt extends Comparison

case object Lte extends Comparison

case object Gte extends Comparison

case object Gt extends Comparison

case object EqualTo extends Comparison

case object NotEqualTo extends Comparison
