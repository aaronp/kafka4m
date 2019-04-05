package kafkaquery.admin

import kafkaquery.core.BaseEndpoint
import kafkaquery.users.{CreateUserRequest, CreateUserResponse}

trait CreateUserEndpoints extends BaseEndpoint {

  def createUserRequest: Request[CreateUserRequest] = {
    post(path / "createUser", jsonRequest[CreateUserRequest]())
  }
  def createUserResponse = jsonResponse[CreateUserResponse]()

  val createUserEndpoint: Endpoint[CreateUserRequest, CreateUserResponse]                     = endpoint(createUserRequest, createUserResponse)
  implicit lazy val `JsonSchema[CreateUserRequest]` : JsonSchema[CreateUserRequest]   = genericJsonSchema
  implicit lazy val `JsonSchema[CreateUserResponse]` : JsonSchema[CreateUserResponse] = genericJsonSchema
}
