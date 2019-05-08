package pipelines.core

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, HCursor}

class DecodeString[A](value: String, constant: A) extends Decoder[A] {
  override def apply(c: HCursor): Result[A] = {
    c.as[String] match {
      case Right(`value`) => Right(constant)
      case Right(other)   => Left(DecodingFailure(s"Expected '$value' but got '$other'", c.history))
      case Left(err)      => Left(err)
    }
  }
}
