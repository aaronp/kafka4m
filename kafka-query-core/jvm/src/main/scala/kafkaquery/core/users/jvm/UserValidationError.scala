package kafkaquery.core.users.jvm

sealed trait UserValidationError

case class UserAlreadyExistsWithName(userName: String) extends UserValidationError
case class UserAlreadyExistsWithEmail(email: String)   extends UserValidationError
case class InvalidEmailAddress(email: String)          extends UserValidationError
