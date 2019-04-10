package kafkaquery

import org.apache.avro.generic.{GenericRecord, IndexedRecord}

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

  type Record    = IndexedRecord // SpecificRecordBase, GenericRecord
  type Predicate = Record => Boolean

  def parseRule(rule: String): Predicate = Expressions.Predicate(rule)

  implicit def asDynamic(msg: Record): DynamicAvroRecord = new DynamicAvroRecord(msg)
}
