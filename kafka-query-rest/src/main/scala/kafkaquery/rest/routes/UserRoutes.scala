package kafkaquery.rest.routes

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives.respondWithHeader
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._

import kafkaquery.rest.jwt.{Claims, Hmac256, JsonWebToken}
import io.circe.Encoder
import javax.crypto.spec.SecretKeySpec
import kafkaquery.users.{LoginRequest, LoginResponse, UserEndpoints}

object UserRoutes {

  def apply(secret: String)(doLogin: LoginRequest => Option[Claims]): UserRoutes = {
    apply(Hmac256.asSecret(secret))(doLogin)
  }

  def apply(secret: SecretKeySpec)(doLogin: LoginRequest => Option[Claims]): UserRoutes = {
    new UserRoutes(secret, doLogin)
  }
}

class UserRoutes(secret: SecretKeySpec, doLogin: LoginRequest => Option[Claims]) extends UserEndpoints with BaseRoutes {

  override def loginResponse: LoginResponse => Route = { resp: LoginResponse =>
    implicit def encoder: Encoder[LoginResponse] = implicitly[JsonSchema[LoginResponse]].encoder
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
    Directives.extractRequest { rqt =>
      // An anonymous user may have tried to browse a page which requires login (e.g. a JWT token), and so upon a successful login,
      // we should redirect to the URL as specified by the 'redirectToHeader' (if set)
      redirectHeader { redirectToHeader: Option[String] =>
        loginEndpoint.implementedBy {
          case (loginRequest, redirectToIn) =>
            doLogin(loginRequest) match {
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

  def routes: Route = loginRoute
}
