package esa.core.users

import java.time.ZonedDateTime

import io.circe
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.circe.java8.time._

case class User(name : String, email : String, created : ZonedDateTime) {
  def jsonStr = {
    this.asJson.noSpaces
  }
}

object User {
  type Id = Long

  def fromJson(json : String): Either[circe.Error, User] = {
    decode[User](json)
  }
}