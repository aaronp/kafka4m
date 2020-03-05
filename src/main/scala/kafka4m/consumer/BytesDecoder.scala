package kafka4m.consumer

import java.nio.charset.StandardCharsets

import kafka4m.Bytes

/**
  * If we have a BytesDecoder, then we can have an [[RecordDecoder]].
  * This shit chains.
  *
  * @tparam A the result type
  */
trait BytesDecoder[A] {
  def decode(bytes: kafka4m.Bytes): A
}

object BytesDecoder {
  def apply[A](implicit instance: BytesDecoder[A]): BytesDecoder[A] = instance

  def lift[A](f: kafka4m.Bytes => A): BytesDecoder[A] = new BytesDecoder[A] {
    override def decode(bytes: Bytes): A = f(bytes)
  }

  implicit object StringDecoder extends BytesDecoder[String] {
    override def decode(bytes: Bytes): String = new String(bytes, StandardCharsets.UTF_8)
  }
}
