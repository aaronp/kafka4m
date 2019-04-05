package esa.rest.jwt
import java.util.Base64

import io.circe
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

case class JwtHeader(algorithm: String, contentType: String) {

  def toJsonBase64: String = {
    if (this == JwtHeader.hs256) {
      JwtHeader.hs256JsonBase64
    } else {
      Base64.getUrlEncoder.encodeToString(JwtHeader.encoder(this).noSpaces.getBytes("UTF-8"))
    }
  }
}

object JwtHeader {

  def fromJson(json: String): Either[circe.Error, JwtHeader] = {
    decode[JwtHeader](json)
  }

  val hs256             = JwtHeader("HS256", "JWT")
  def hs256Json: String = hs256.asJson.noSpaces
  lazy val hs256JsonBase64: String = {
    Base64.getUrlEncoder.encodeToString(hs256Json.getBytes("UTF-8"))
  }

  implicit val encoder: Encoder[JwtHeader] = Encoder.forProduct2("alg", "typ")(JwtHeader.unapply(_).get)
  implicit val decoder: Decoder[JwtHeader] = Decoder.forProduct2("alg", "typ")(JwtHeader.apply)
}
