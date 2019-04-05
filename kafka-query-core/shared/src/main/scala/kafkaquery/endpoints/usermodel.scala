package kafkaquery.endpoints

final case class CreateUserRequest(user : String, email : String, password : String)
final case class CreateUserResponse(ok : Boolean, jwtToken : Option[String])

final case class LoginRequest(user : String, password : String)
final case class LoginResponse(ok : Boolean, jwtToken : Option[String], redirectTo : Option[String])
