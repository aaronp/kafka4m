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

  type Record = IndexedRecord

  def asAvroPredicate(rule: String): AvroExpressions.Predicate = AvroExpressions.Predicate(rule)

}
