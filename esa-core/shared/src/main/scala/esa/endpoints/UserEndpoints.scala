package esa.endpoints
import endpoints.{algebra, generic}

trait UserEndpoints extends algebra.Endpoints with algebra.JsonSchemaEntities with generic.JsonSchemas {

  def redirectHeader: RequestHeaders[Option[String]] = {
    optHeader(
      "redirectTo",
      Option(
        "Routes requiring authentication may redirect to login, in which case a successful " +
          "login will return a temporary redirect response to this route in addition to an X-Access-Token containing tokens to use in subsequent requests")
    )
  }

  def loginRequest: Request[(LoginRequest, Option[String])] = {
    post(path / "login", jsonRequest[LoginRequest](), redirectHeader)
  }

  def loginResponse = jsonResponse[LoginResponse](Option("A login response which will also include an X-Access-Token header to use in subsequent requests"))

  /**
    * Get the counter current value.
    * Uses the HTTP verb “GET” and URL path “/current-value”.
    * The response entity is a JSON document representing the counter value.
    */
  def login: Endpoint[(LoginRequest, Option[String]), LoginResponse] = endpoint(loginRequest, loginResponse)


  def createUserRequest: Request[CreateUserRequest] = {
    post(path / "createUser", jsonRequest[CreateUserRequest]())
  }
  def createUserResponse = jsonResponse[CreateUserResponse]()

  def createUser = endpoint(createUserRequest, createUserResponse)

  implicit lazy val `JsonSchema[CreateUserRequest]`: JsonSchema[CreateUserRequest]   = genericJsonSchema
  implicit lazy val `JsonSchema[CreateUserResponse]`: JsonSchema[CreateUserResponse]   = genericJsonSchema

  implicit lazy val `JsonSchema[LoginRequest]`: JsonSchema[LoginRequest]   = genericJsonSchema
  implicit lazy val `JsonSchema[LoginResponse]`: JsonSchema[LoginResponse] = genericJsonSchema
}