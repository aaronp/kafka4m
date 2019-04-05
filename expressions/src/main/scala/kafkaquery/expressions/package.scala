package kafkaquery

import org.apache.avro.specific.SpecificRecordBase

/**
  * The main entry point:
  *
  * {{{
  *   val rule = kafkaquery.expressions.parseRule("value.id == 12 || value.amount <= 12.345")
  *
  *   ...
  *   dataStream.filter(rule)
  * }}}
  */
package object expressions {

  type Record    = SpecificRecordBase
  type Predicate = Record => Boolean

  def parseRule(rule: String): Predicate = Expressions.Predicate(rule)

  implicit def asDynamic(msg: SpecificRecordBase): DynamicAvroRecord = new DynamicAvroRecord(msg)
}
