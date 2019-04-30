package pipelines.users

import io.circe.Decoder.Result
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.duration.FiniteDuration

case class UserDetails(email: Email, sessionDuration: FiniteDuration)
object UserDetails {
  implicit def encoder = deriveEncoder[UserDetails]
  implicit def decoder = deriveDecoder[UserDetails]
}

final case class CreateUserRequest(user: UserName, email: Email, password: String)
object CreateUserRequest {
  implicit def encoder = deriveEncoder[CreateUserRequest]
  implicit def decoder = deriveDecoder[CreateUserRequest]
}

final case class CreateUserResponse(ok: Boolean, jwtToken: Option[String])
object CreateUserResponse {
  implicit def encoder = deriveEncoder[CreateUserResponse]
  implicit def decoder = deriveDecoder[CreateUserResponse]
}

final case class UpdateUserRequest(userOrEmail: Either[UserName, Email], details: UserDetails)
object UpdateUserRequest {
  implicit val encoder: Encoder[UpdateUserRequest] = {
    Encoder.instance[UpdateUserRequest] { request =>
      import io.circe.syntax._
      request.userOrEmail match {
        case Left(user) =>
          Json.obj("user" -> Json.fromString(user), "details" -> request.details.asJson)
        case Right(email) =>
          Json.obj("email" -> Json.fromString(email), "details" -> request.details.asJson)
      }
    }
  }
  implicit val decoder: Decoder[UpdateUserRequest] = Decoder.instance[UpdateUserRequest] { cursor =>
    val userEither: Result[Either[UserName, Email]] = {
      import cats.syntax.either._
      cursor.downField("user").as[String].map(Left.apply).orElse {
        cursor.downField("email").as[String].map(Right.apply)
      }
    }

    for {
      user    <- userEither
      details <- cursor.downField("details").as[UserDetails]
    } yield {
      UpdateUserRequest(user, details)
    }
  }
}

final case class UpdateUserResponse(ok: Boolean)
object UpdateUserResponse {
  implicit def encoder = deriveEncoder[UpdateUserResponse]
  implicit def decoder = deriveDecoder[UpdateUserResponse]
}

final case class LoginRequest(user: UserName, password: String)
object LoginRequest {
  implicit def encoder = deriveEncoder[LoginRequest]
  implicit def decoder = deriveDecoder[LoginRequest]
}

final case class LoginResponse(ok: Boolean, jwtToken: Option[String], redirectTo: Option[String])
object LoginResponse {
  implicit def encoder = deriveEncoder[LoginResponse]
  implicit def decoder = deriveDecoder[LoginResponse]
}
