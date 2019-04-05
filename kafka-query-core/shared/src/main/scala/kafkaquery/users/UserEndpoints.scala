package kafkaquery.users

import kafkaquery.admin.{CreateUserEndpoints, LoginEndpoints}

/**
  * Endpoints for user actions (e.g. login, logout, update profile, ...)
  */
trait UserEndpoints extends LoginEndpoints with CreateUserEndpoints{

  def userEndpoints = List(loginEndpoint, createUserEndpoint)

}
