package kafkaquery.expresssions.ast

sealed trait AndOr

case object And extends AndOr

case object Or extends AndOr
