package pipelines.data

import java.nio.ByteBuffer
import java.nio.file.Path

import eie.io._
import io.circe.Json
import pipelines.core.DataType

import scala.reflect.ClassTag

/**
  * Represents the pieces required to write data down to disk locally
  *
  * @param dir the parent directory to contain the records
  */
class PersistLocation(val dir: Path) {
  def toBytes[A](typ: DataType)(implicit c1assTag: ClassTag[A]): Option[ToBytes[A]] = PersistLocation.toBytes[A](typ)
}

object PersistLocation {
  private val CirceJsonClass = classOf[Json]
  private val StringClass    = classOf[String]
  private val IntClass       = classOf[Int]
  private val LongClass      = classOf[Long]
  private val DoubleClass    = classOf[Double]
  private val ArrayClass     = classOf[Array[Byte]]

  /** @param typ
    * @param c1assTag
    * @tparam A
    * @return a means to turn a type A into a ToBytes[A] instance
    */
  def toBytes[A](typ: DataType)(implicit c1assTag: ClassTag[A]): Option[ToBytes[A]] = {
    c1assTag.runtimeClass match {
      case CirceJsonClass => Option(JsonToBytes.asInstanceOf[ToBytes[A]])
      case StringClass    => Option(ToBytes.Utf8String.asInstanceOf[ToBytes[A]])
      case ArrayClass     => Option(ByteArrayToBytes.asInstanceOf[ToBytes[A]])
      case IntClass       => Option(IntToBytes.asInstanceOf[ToBytes[A]])
      case LongClass      => Option(LongToBytes.asInstanceOf[ToBytes[A]])
      case DoubleClass    => Option(IntToBytes.asInstanceOf[ToBytes[A]])
      case _              => None
    }
  }

  def apply(dir: Path): PersistLocation = {
    new PersistLocation(dir)
  }

  def JsonToBytes = ByteArrayToBytes.contramap[Json](_.noSpaces.getBytes("UTF-8"))

  object ByteArrayToBytes extends ToBytes[Array[Byte]] {
    override def bytes(value: Array[Byte]): Array[Byte] = value
  }

  object IntToBytes extends ToBytes[Int] {
    override def bytes(value: Int): Array[Byte] = {
      ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(value).array()
    }
  }
  object LongToBytes extends ToBytes[Long] {
    override def bytes(value: Long): Array[Byte] = {
      ByteBuffer.allocate(java.lang.Long.BYTES).putLong(value).array()
    }
  }
  object DoubleToBytes extends ToBytes[Double] {
    override def bytes(value: Double): Array[Byte] = {
      ByteBuffer.allocate(java.lang.Double.BYTES).putDouble(value).array()
    }
  }
}
