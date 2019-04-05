package kafkaquery.expresssions.ast

sealed trait Expression

case class ConstExpression(value: Term) extends Expression

case class PredicateExpression(predicate: Predicate) extends Expression

case class EvalExpression(lhs: Expression, op: Op, rhs: Expression) extends Expression
