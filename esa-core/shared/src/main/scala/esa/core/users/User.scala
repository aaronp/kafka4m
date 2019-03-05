package esa.core.users

import java.time.ZonedDateTime

import io.circe
import io.circe.generic.auto
import io.circe.java8.time.{JavaTimeDecoders, JavaTimeEncoders}
import io.circe.parser._
import io.circe.{Decoder, Encoder}

case class User(name: String, email: String, created: ZonedDateTime) {
  def jsonStr: String = User.encoder(this).noSpaces
}

object User extends JavaTimeEncoders with JavaTimeDecoders {
  type Id = String

  implicit val encoder: Encoder[User] = auto.exportEncoder[User].instance
  implicit val decoder: Decoder[User] = auto.exportDecoder[User].instance

  def fromJson(json: String): Either[circe.Error, User] = {
    decode[User](json)
  }
}
