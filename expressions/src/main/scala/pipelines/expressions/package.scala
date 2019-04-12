package pipelines

import org.apache.avro.generic.IndexedRecord

/**
  * The main entry point:
  *
  * {{{
  *   val rule = pipelines.expressions.parseRule("value.id == 12 || value.amount <= 12.345")
  *
  *   ...
  *   dataStream.filter(rule)
  * }}}
  */
package object expressions extends LowPriorityExpressionImplicits {

  type Record    = IndexedRecord // SpecificRecordBase, GenericRecord
  type Predicate = Record => Boolean

  val PassThrough: Predicate = (_ => true)

  def parseRule(rule: String): Predicate = Expressions.Predicate(rule)

}
