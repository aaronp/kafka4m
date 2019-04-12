package pipelines.expresssions.ast

sealed trait Predicate

final case class ConstPredicate(value: Boolean)                                           extends Predicate
final case class EvalPredicate(lhs: Expression, comp: Comparison, rhs: Expression)        extends Predicate
final case class ConjunctionPredicate(lhs: Predicate, conjunction: AndOr, rhs: Predicate) extends Predicate
