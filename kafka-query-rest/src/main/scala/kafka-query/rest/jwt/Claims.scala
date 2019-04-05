package esa.rest.jwt
import java.time.ZonedDateTime
import java.util.Base64

import io.circe
import io.circe.Decoder.Result
import io.circe._
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration

// https://en.m.wikipedia.org/wiki/JSON_Web_Token
case class Claims(
    name: String = null,
    iss: String = null,
    sub: String = null,
    aud: String = null,
    exp: Claims.NumericDate = 0L,
    nbf: Claims.NumericDate = 0L,
    iat: Claims.NumericDate = 0L,
    jti: String = null
) {

  def isExpired(now: ZonedDateTime) = {
    exp != 0 && exp <= Claims.asNumericDate(now)
  }

  def toJson: String = Claims.toJson(this)

  def toJsonBase64: String = {
    val bytes = Base64.getUrlEncoder.encode(toJson.getBytes("UTF-8"))
    new String(bytes, "UTF-8")
  }
}

object Claims extends io.circe.java8.time.JavaTimeEncoders with io.circe.java8.time.JavaTimeDecoders {

  type EpochSeconds = Long

  type NumericDate = EpochSeconds

  def asNumericDate(d8: ZonedDateTime) = d8.toEpochSecond

  implicit object ClaimsDecoder extends Decoder[Claims] {
    override def apply(c: HCursor): Result[Claims] = {
      def fld[A: Decoder](key: String, default: A) = c.downField(key).success.flatMap(_.as[A].toOption).getOrElse(default)

      Right(
        new Claims(
          name = fld[String]("name", null),
          iss = fld[String]("iss", null),
          sub = fld[String]("sub", null),
          aud = fld[String]("aud", null),
          exp = fld[Long]("exp", 0),
          nbf = fld[Long]("nbf", 0),
          iat = fld[Long]("iat", 0),
          jti = fld[String]("jti", null),
        ))
    }
  }
  implicit object ClaimsEncoder extends Encoder[Claims] {
    override def apply(c: Claims): Json = {
      import c._

      val stringMap = Map(
        "name" -> name,
        "iss"  -> iss,
        "sub"  -> sub,
        "aud"  -> aud,
        "jti"  -> jti
      ).filter {
          case (_, null) => false
          case (_, "")   => false
          case _         => true
        }
        .mapValues(Json.fromString)

      val numMap = Map(
        "nbf" -> nbf,
        "iat" -> iat,
        "exp" -> exp
      ).filter {
          case (_, 0L) => false
          case _       => true
        }
        .mapValues(Json.fromLong)

      val obj = JsonObject.fromMap(stringMap ++ numMap)
      Json.fromJsonObject(obj)
    }
  }

  def after(expiry: FiniteDuration, now: ZonedDateTime = ZonedDateTime.now()) = new {

    def forUser(name: String): Claims = {
      val expires = now.plusNanos(expiry.toNanos)
      new Claims(name = name, exp = asNumericDate(expires))
    }
  }

  def toJson(c: Claims): String = {
    c.asJson.noSpaces
  }

  def fromJson(j: String): Either[circe.Error, Claims] = {
    decode[Claims](j)
  }

}
