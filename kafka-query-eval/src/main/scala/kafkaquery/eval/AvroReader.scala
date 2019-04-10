package kafkaquery.eval

import java.nio.file.Path

import kafkaquery.connect.Bytes
import kafkaquery.expressions.{Predicate, Record}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.{BinaryDecoder, DatumReader, DecoderFactory}
import org.apache.avro.specific.SpecificDatumReader

import scala.reflect.ClassTag

case class AvroReader(reader: DatumReader[_ <: Record], decoderFactory: DecoderFactory = org.apache.avro.io.DecoderFactory.get) {

  def read(bytes: Bytes): Record = {
    val d8a: BinaryDecoder = decoderFactory.binaryDecoder(bytes, null)
    reader.read(null, d8a)
  }

  def matches(bytes: Bytes, expression: String) = {
    val predicate: Predicate = kafkaquery.expressions.Expressions.cache(expression)
    val value                = read(bytes)
    predicate(value)
  }
}

object AvroReader {

  def apply[T <: Record: ClassTag]: AvroReader = {
    val c1ass: Class[Record]                = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[Record]]
    val reader: SpecificDatumReader[Record] = new SpecificDatumReader[Record](c1ass)
    new AvroReader(reader)
  }

  def apply(schema: Schema): AvroReader = {
    val reader: GenericDatumReader[GenericRecord] = new GenericDatumReader[GenericRecord](schema)
    new AvroReader(reader)
  }

  def apply(pathToSchema: Path): AvroReader = {
    import eie.io._
    apply(pathToSchema.text)
  }

  def apply(schemaContent: String): AvroReader = {
    apply(new Schema.Parser().parse(schemaContent))
  }

}
