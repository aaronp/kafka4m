package kafkaquery.endpoints

/**
  * Endpoints for user actions (e.g. login, logout, update profile, ...)
  */
trait UserEndpoints extends LoginEndpoints with CreateUserEndpoints{

  def userEndpoints = List(loginEndpoint, createUserEndpoint)

}
