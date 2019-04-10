package kafkaquery.eval

import java.nio.file.Path

import kafkaquery.connect.Bytes
import kafkaquery.expressions.{Predicate, Record}
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.{BinaryDecoder, DatumReader, DecoderFactory}
import org.apache.avro.specific.SpecificDatumReader

import scala.reflect.ClassTag

case class AvroReader[A <: Record](reader: DatumReader[A], decoderFactory: DecoderFactory = org.apache.avro.io.DecoderFactory.get) {

  def read(bytes: Bytes): A = {
    val d8a: BinaryDecoder = decoderFactory.binaryDecoder(bytes, null)
    val record : A = null.asInstanceOf[A]
    reader.read(record, d8a)
  }

  def matches(bytes: Bytes, expression: String) = {
    val predicate: Predicate = kafkaquery.expressions.Expressions.cache(expression)
    val value                = read(bytes)
    predicate(value)
  }
}

object AvroReader {

  def apply[A <: Record: ClassTag]: AvroReader[A] = {
    val rtc = implicitly[ClassTag[A]].runtimeClass
    val c1ass: Class[A]                = rtc.asInstanceOf[Class[A]]
    val reader: SpecificDatumReader[A] = new SpecificDatumReader[A](c1ass)
    new AvroReader[A](reader)
  }

  def apply(schema: Schema): AvroReader[GenericRecord] = {
    val reader: GenericDatumReader[GenericRecord] = new GenericDatumReader[GenericRecord](schema)
    new AvroReader(reader)
  }

  def apply(pathToSchema: Path): AvroReader[GenericRecord] = {
    import eie.io._
    apply(pathToSchema.text)
  }

  def apply(schemaContent: String): AvroReader[GenericRecord] = {
    apply(new Schema.Parser().parse(schemaContent))
  }

}
