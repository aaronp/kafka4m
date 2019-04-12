package pipelines.admin

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import pipelines.core.BaseEndpoint
import pipelines.users.{CreateUserRequest, CreateUserResponse}

trait CreateUserEndpoints extends BaseEndpoint {

  def createUserRequest: Request[CreateUserRequest] = {
    post(path / "createUser", jsonRequest[CreateUserRequest]())
  }
  def createUserResponse = jsonResponse[CreateUserResponse]()

  val createUserEndpoint: Endpoint[CreateUserRequest, CreateUserResponse]                     = endpoint(createUserRequest, createUserResponse)
  implicit lazy val `JsonSchema[CreateUserRequest]` : JsonSchema[CreateUserRequest]   = JsonSchema(deriveEncoder[CreateUserRequest], deriveDecoder[CreateUserRequest])
  implicit lazy val `JsonSchema[CreateUserResponse]` : JsonSchema[CreateUserResponse] = JsonSchema(deriveEncoder[CreateUserResponse], deriveDecoder[CreateUserResponse])
}
