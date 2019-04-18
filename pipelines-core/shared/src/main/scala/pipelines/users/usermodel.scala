package pipelines.users

import io.circe.{Decoder, ObjectEncoder}
import io.circe.generic.semiauto._

final case class CreateUserRequest(user : String, email : String, password : String)
object CreateUserRequest {
  implicit def encoder: ObjectEncoder[CreateUserRequest] = deriveEncoder[CreateUserRequest]
  implicit def decoder: Decoder[CreateUserRequest] = deriveDecoder[CreateUserRequest]
}


final case class CreateUserResponse(ok : Boolean, jwtToken : Option[String])
object CreateUserResponse {
  implicit def encoder: ObjectEncoder[CreateUserResponse] = deriveEncoder[CreateUserResponse]
  implicit def decoder: Decoder[CreateUserResponse] = deriveDecoder[CreateUserResponse]
}


final case class LoginRequest(user : String, password : String)
object LoginRequest {
  implicit def encoder: ObjectEncoder[LoginRequest] = deriveEncoder[LoginRequest]
  implicit def decoder: Decoder[LoginRequest] = deriveDecoder[LoginRequest]
}

final case class LoginResponse(ok : Boolean, jwtToken : Option[String], redirectTo : Option[String])
object LoginResponse {
  implicit def encoder: ObjectEncoder[LoginResponse] = deriveEncoder[LoginResponse]
  implicit def decoder: Decoder[LoginResponse] = deriveDecoder[LoginResponse]
}

