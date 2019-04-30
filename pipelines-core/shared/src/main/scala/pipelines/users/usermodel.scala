package pipelines.users

import io.circe.generic.semiauto._

import scala.concurrent.duration.FiniteDuration

case class UserDetails(email: Email, sessionDuration : FiniteDuration)

final case class CreateUserRequest(UserName, email: Email, password: String)
object CreateUserRequest {
  implicit def encoder  = deriveEncoder[CreateUserRequest]
  implicit def decoder     = deriveDecoder[CreateUserRequest]
}

final case class CreateUserResponse(ok: Boolean, jwtToken: Option[String])
object CreateUserResponse {
  implicit def encoder  = deriveEncoder[CreateUserResponse]
  implicit def decoder     = deriveDecoder[CreateUserResponse]
}

final case class UpdateUserRequest(userOrEmail: Either[UserName, Email], details : UserDetails)
object UpdateUserRequest {
  implicit def encoder  = deriveEncoder[UpdateUserRequest]
  implicit def decoder     = deriveDecoder[UpdateUserRequest]
}

final case class UpdateUserResponse(ok: Boolean)
object UpdateUserResponse {
  implicit def encoder = deriveEncoder[UpdateUserResponse]
  implicit def decoder    = deriveDecoder[UpdateUserResponse]
}

final case class LoginRequest(UserName, password: String)
object LoginRequest {
  implicit def encoder = deriveEncoder[LoginRequest]
  implicit def decoder   = deriveDecoder[LoginRequest]
}

final case class LoginResponse(ok: Boolean, jwtToken: Option[String], redirectTo: Option[String])
object LoginResponse {
  implicit def encoder = deriveEncoder[LoginResponse]
  implicit def decoder  = deriveDecoder[LoginResponse]
}
