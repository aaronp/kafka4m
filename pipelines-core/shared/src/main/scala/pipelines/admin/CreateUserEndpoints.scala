package pipelines.admin

import pipelines.core.BaseEndpoint
import pipelines.users.{CreateUserRequest, CreateUserResponse}

trait CreateUserEndpoints extends BaseEndpoint {

  def createUserRequest(implicit req: JsonRequest[CreateUserRequest]): Request[CreateUserRequest] = {
    post(path / "users" / "create", jsonRequest[CreateUserRequest]())
  }
  def createUserResponse(implicit resp: JsonResponse[CreateUserResponse]): Response[CreateUserResponse] = jsonResponse[CreateUserResponse]()

  def createUserEndpoint(implicit req: JsonRequest[CreateUserRequest], resp: JsonResponse[CreateUserResponse]): Endpoint[CreateUserRequest, CreateUserResponse]                     = endpoint(createUserRequest, createUserResponse)
}
