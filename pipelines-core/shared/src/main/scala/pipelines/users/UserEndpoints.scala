package pipelines.users

import pipelines.admin.{CreateUserEndpoints, LoginEndpoints}

/**
  * Endpoints for user actions (e.g. login, logout, update profile, ...)
  */
trait UserEndpoints extends LoginEndpoints with CreateUserEndpoints{

  def userEndpoints = List(loginEndpoint, createUserEndpoint)

}
