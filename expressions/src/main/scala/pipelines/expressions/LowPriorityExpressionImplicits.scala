package pipelines.expressions

import pipelines.DynamicAvroRecord
import org.apache.avro.generic.GenericRecord

trait LowPriorityExpressionImplicits {
  implicit def asDynamic(msg: Record): DynamicAvroRecord               = new DynamicAvroRecord(msg)
  implicit def genericAsDynamic(msg: GenericRecord): DynamicAvroRecord = new DynamicAvroRecord(msg)
}
object LowPriorityExpressionImplicits extends LowPriorityExpressionImplicits
