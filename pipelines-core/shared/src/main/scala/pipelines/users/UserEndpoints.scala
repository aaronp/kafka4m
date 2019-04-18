package pipelines.users

import pipelines.admin.{CreateUserEndpoints, LoginEndpoints}

/**
  * Endpoints for user actions (e.g. login, logout, update profile, ...)
  */
trait UserEndpoints extends LoginEndpoints with CreateUserEndpoints {

  def userEndpoints(implicit req1: JsonRequest[LoginRequest], resp1: JsonResponse[LoginResponse], req2: JsonRequest[CreateUserRequest], resp2: JsonResponse[CreateUserResponse]) =
    List(loginEndpoint, createUserEndpoint)

}
