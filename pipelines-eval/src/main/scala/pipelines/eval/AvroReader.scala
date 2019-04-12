package pipelines.eval

import java.nio.file.Path

import pipelines.DynamicAvroRecord
import pipelines.expressions.Record
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.message.BinaryMessageDecoder
import org.apache.avro.specific.SpecificData

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Represents something which can read avro records from byte arrays
  *
  * @tparam A
  */
trait AvroReader[A] {
  def read(bytes: Bytes): Try[A]
}

object AvroReader {

  def apply[A <: Record: ClassTag]: AvroReader[A] = {
    val rtc             = implicitly[ClassTag[A]].runtimeClass
    val c1ass: Class[A] = rtc.asInstanceOf[Class[A]]

    // generated stubs have this static method
    val getClassSchemaMethod = c1ass.getDeclaredMethod("getClassSchema")
    val readSchema: Schema   = getClassSchemaMethod.invoke(null).asInstanceOf[org.apache.avro.Schema]
    apply[A](readSchema)
  }

  def apply[A](schema: Schema): AvroReader[A] = {
    val data                            = new SpecificData
    val binDec: BinaryMessageDecoder[A] = new BinaryMessageDecoder[A](data, schema)
    new AvroReader[A] {
      override def read(bytes: Bytes): Try[A] = Try(binDec.decode(bytes))
    }
  }

  def forSchema(pathToSchema: Path): AvroReader[GenericRecord] = {
    import eie.io._
    apply(pathToSchema.text)
  }

  def apply[A](schemaContent: String): AvroReader[A] = {
    apply(parseSchema(schemaContent))
  }

  def generic(schemaContent: String): AvroReader[DynamicAvroRecord] = {
    val schema = parseSchema(schemaContent)
    val data   = new GenericData
    val binDec = new BinaryMessageDecoder[GenericRecord](data, schema)
    new AvroReader[DynamicAvroRecord] {
      override def read(bytes: Bytes): Try[DynamicAvroRecord] = Try(new DynamicAvroRecord(binDec.decode(bytes)))
    }
  }

  def parseSchema(schemaContent: String): Schema = new Schema.Parser().parse(schemaContent)

}
