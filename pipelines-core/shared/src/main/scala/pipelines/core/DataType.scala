package pipelines.core

import io.circe.Decoder.Result
import io.circe._

/**
  * A way of explicitly qualifying the types of a data source.
  *
  * This may be a really dumb idea, as it could disagree w/ the actual type.
  *
  * It was done as a way to add some information to something which could otherwise be opaque (e.g. an array of bytes -- is it an array of an avro record, protobuf, gzipped json?)
  *
  * @param name
  */
@deprecated
sealed class DataType(val name: String)

case object ByteArray      extends DataType("byteArray")
case object AvroRecord     extends DataType("avro")
case object JsonRecord     extends DataType("json")
case object ProtobufRecord extends DataType("protobuf")

/** a fallback type for any rich type
  */
case class AnyType(override val name: String) extends DataType(name)

object DataType {

  private val values = Set(
    ByteArray,
    AvroRecord,
    JsonRecord,
    ProtobufRecord
  )
  implicit val encodeEvent: Encoder[DataType] = Encoder.instance {
    case dt: DataType => Json.fromString(dt.name)
  }

  implicit object decodeEvent extends Decoder[DataType] {
    final def apply(c: HCursor): Result[DataType] = c.as[String] match {
      case Right(name) =>
        values.find(_.name == name) match {
          case Some(found) => Right(found)
          case _           => Right(AnyType(name))
        }
      case Left(failure) => Left(failure)
    }
  }
}
