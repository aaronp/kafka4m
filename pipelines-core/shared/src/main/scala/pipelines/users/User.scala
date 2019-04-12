package pipelines.users

import java.time.{ZoneId, ZonedDateTime}

import io.circe
import io.circe.java8.time.{JavaTimeDecoders, JavaTimeEncoders}
import io.circe.parser._
import io.circe.{Decoder, Encoder}

case class User(name: String, email: String, created: ZonedDateTime) {
  def jsonStr: String = User.encoder(this).noSpaces
}

object User extends JavaTimeEncoders with JavaTimeDecoders {
  type Id = String

  implicit val encoder: Encoder[User] = io.circe.generic.auto.exportEncoder[User].instance
  implicit val decoder: Decoder[User] = io.circe.generic.auto.exportDecoder[User].instance

  def create(name: String, email: String, created: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))): User = {
    new User(name, email, created)
  }

  def fromJson(json: String): Either[circe.Error, User] = {
    decode[User](json)
  }
}
