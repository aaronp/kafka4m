package pipelines.core

import java.time

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import io.circe.java8.time.{JavaTimeDecoders, JavaTimeEncoders}

import scala.concurrent.duration._


final case class Rate(messages: Int, per: FiniteDuration) {
  require(messages > 0, s"rate must specify > 0 messages: $messages")
}

object Rate extends JavaTimeEncoders {
  def perSecond(messages: Int) = Rate(messages, 1.second)

  implicit val rateEncoder = Encoder.instance[Rate] { r8 =>
    val jDuration = java.time.Duration.ofMillis(r8.per.toMillis)
    Json.obj("messages" -> Json.fromInt(r8.messages), "per" -> encodeDuration(jDuration))
  }

  implicit object RateDecoder extends Decoder[Rate] with JavaTimeDecoders {
    override def apply(c: HCursor): Result[Rate] = {
      val perResult: Result[time.Duration] = c.downField("per") match {
        case per: HCursor => decodeDuration(per)
        case other        => Left(DecodingFailure("no 'per' field", other.history))
      }

      for {
        msgs     <- c.downField("messages").as[Int]
        perJTime <- perResult
        per = perJTime.toMillis.millis
      } yield {
        Rate(msgs, per)
      }
    }
  }
}
