package pipelines.rest.routes

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives.{respondWithHeader, _}
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import javax.crypto.spec.SecretKeySpec
import pipelines.rest.jwt.{Claims, Hmac256, JsonWebToken}
import pipelines.users._

import scala.concurrent.{ExecutionContext, Future}

object UserRoutes {

  def apply(secret: String)(doLogin: LoginRequest => Future[Option[Claims]])(implicit ec: ExecutionContext): UserRoutes = {
    apply(Hmac256.asSecret(secret))(doLogin)
  }

  def apply(secret: SecretKeySpec)(doLogin: LoginRequest => Future[Option[Claims]])(implicit ec: ExecutionContext): UserRoutes = {
    new UserRoutes(secret, doLogin)
  }
}

class UserRoutes(secret: SecretKeySpec, doLogin: LoginRequest => Future[Option[Claims]])(implicit ec: ExecutionContext) extends UserEndpoints with BaseCirceRoutes {

  override def loginResponse(implicit resp: JsonResponse[LoginResponse]): LoginResponse => Route = { resp: LoginResponse =>
//    implicit def encoder: Encoder[LoginResponse] = implicitly[JsonSchema[LoginResponse]].encoder
    resp.jwtToken match {
      case Some(token) =>
        respondWithHeader(RawHeader("X-Access-Token", token)) {
          resp.redirectTo match {
            case Some(uriString) =>
              Directives.redirect(Uri(uriString), StatusCodes.TemporaryRedirect)
            case None =>
              Directives.complete(resp)
          }
        }
      case None =>
        Directives.complete(resp)
    }
  }

  def loginRoute: Route = {

    implicit def loginRequestSchema: JsonSchema[LoginRequest]   = JsonSchema(implicitly, implicitly)
    implicit def loginResponseSchema: JsonSchema[LoginResponse] = JsonSchema(implicitly, implicitly)

    Directives.extractRequest { rqt =>
      // An anonymous user may have tried to browse a page which requires login (e.g. a JWT token), and so upon a successful login,
      // we should redirect to the URL as specified by the 'redirectToHeader' (if set)
      redirectHeader { redirectToHeader: Option[String] =>
        loginEndpoint.implementedByAsync {
          case (loginRequest, redirectToIn) =>
            doLogin(loginRequest).map {
              case Some(claims) =>
                val redirectTo: Option[String] = redirectToIn.orElse(redirectToHeader).orElse {
                  rqt.uri.queryString().flatMap { rawQueryStr =>
                    Query(rawQueryStr).get("redirectTo")
                  }
                }
                val token = JsonWebToken.asHmac256Token(claims, secret)
                LoginResponse(true, Option(token), redirectTo)

              case None =>
                LoginResponse(false, None, None)
            }
        }
      }
    }
  }

  def createUserRoute: Route = {

    implicit def createUserRequestSchema: JsonSchema[CreateUserRequest]   = JsonSchema(implicitly, implicitly)
    implicit def createUserResponseSchema: JsonSchema[CreateUserResponse] = JsonSchema(implicitly, implicitly)

    createUserEndpoint.implementedBy { req =>
      ???
    }
  }

  def routes: Route = loginRoute ~ createUserRoute
}
