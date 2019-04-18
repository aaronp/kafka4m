package pipelines.admin

import pipelines.core.BaseEndpoint
import pipelines.users.{LoginRequest, LoginResponse}

trait LoginEndpoints extends BaseEndpoint {

  def redirectHeader: RequestHeaders[Option[String]] = {
    optHeader(
      "redirectTo",
      Option(
        "Routes requiring authentication may redirect to login, in which case a successful " +
          "login will return a temporary redirect response to this route in addition to an X-Access-Token containing tokens to use in subsequent requests")
    )
  }

  def loginRequest(implicit req: JsonRequest[LoginRequest]): Request[(LoginRequest, Option[String])] = {
    post(path / "users" / "login", jsonRequest[LoginRequest](Option("Basic user login request")), redirectHeader)
  }

  def loginResponse(implicit resp : JsonResponse[LoginResponse]): Response[LoginResponse] = jsonResponse[LoginResponse](Option("A login response which will also include an X-Access-Token header to use in subsequent requests"))

  /**
    * Get the counter current value.
    * Uses the HTTP verb “GET” and URL path “/current-value”.
    * The response entity is a JSON document representing the counter value.
    */
  def loginEndpoint(implicit req: JsonRequest[LoginRequest], resp : JsonResponse[LoginResponse]): Endpoint[(LoginRequest, Option[String]), LoginResponse] = endpoint(loginRequest, loginResponse)
}
