package kafkaquery.rest.routes
import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model.{headers, _}
import akka.http.scaladsl.model.headers.{Authorization, HttpChallenges, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.server.Directives.respondWithHeader
import akka.http.scaladsl.server._
import kafkaquery.rest.jwt.JsonWebToken.JwtError
import kafkaquery.rest.jwt.{Claims, JsonWebToken}
import javax.crypto.spec.SecretKeySpec

/**
  * Mixing in this trait will offer an 'authenticated' directive to allow routes to require authentication.
  *
  * If these routes match but do not have a valid [[JsonWebToken]] then they will get either an Unauthorized status code
  * or be temporarily redirected to the login page (as determined by the 'loginUri' function) with a 'redirectTo' query
  * parameter specified w/ the original uri so that, upon successful login, the user can then be redirected to their original
  * intended endpoint.
  *
  * @see endpoints.akkahttp.server.BasicAuthentication
  */
trait AuthenticatedDirective {

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials rejects request
    */
  lazy val authenticated: Directive1[Claims] = {
    Directives.optionalHeaderValue(extractCredentials).flatMap {
      case Some(jwtTokenString) =>
        JsonWebToken.forToken(jwtTokenString, secret) match {
          case Right(jwt) if !isExpired(jwt) => onValidToken(jwtTokenString, jwt)
          case Right(jwt)                    => onExpiredToken(jwt)
          case Left(err)                     => onInvalidToken(err)
        }
      case None => onMissingToken()
    }
  }

  /** @return the secret to use to sign JWTokens
    */
  protected def secret: SecretKeySpec

  /** @return the Bearer auth realm
    */
  protected def realm: String

  /** @return the current time. This is exposed to allow implementations to have specific control, such as in tests
    */
  protected def now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))

  protected def isExpired(jwt: JsonWebToken): Boolean = jwt.claims.isExpired(now)

  /**
    * @param intendedPath the original URI the user was trying to access
    * @return a login URI which may contain a 'redirectTo' query paramter which specifies this intendecPath
    */
  def loginUri(intendedPath: Uri): Uri

  /** provides a means to update a token - e.g. perhaps by resetting its expiry on access
    *
    * @param originalJwtToken the input JWT as a string, for convenience/performance should we simply want to return and use the same token again
    * @param token the parsed JWT
    * @param secret the secret to use to encode an updated token
    * @return an updated json web token
    */
  protected def updateTokenOnAccess(originalJwtToken: String, token: JsonWebToken, secret: SecretKeySpec): String = {
    originalJwtToken
  }

  // continue to return the token
  protected def onValidToken(jwtTokenString: String, jwt: JsonWebToken): Directive[Tuple1[Claims]] = {
    val newToken = updateTokenOnAccess(jwtTokenString, jwt, secret)
    respondWithHeader(RawHeader("X-Access-Token", newToken)).tflatMap { _ =>
      Directives.provide(jwt.claims)
    }
  }

  protected def onMissingToken(): Directive[Tuple1[Claims]] = {
    Directive[Tuple1[Claims]] { _ =>
      Directives.complete(
        HttpResponse(
          StatusCodes.Unauthorized,
          scala.collection.immutable.Seq[HttpHeader](headers.`WWW-Authenticate`(HttpChallenges.oAuth2(realm)))
        ))
    }
  }

  protected def onInvalidToken(err: JwtError): Directive[Tuple1[Claims]] = {
    Directives.complete(
      HttpResponse(
        StatusCodes.Unauthorized,
        scala.collection.immutable.Seq[HttpHeader](headers.`WWW-Authenticate`(HttpChallenges.oAuth2(realm)))
      ))
  }

  // it's valid, but expired. We redirect to login
  protected def onExpiredToken(jwt: JsonWebToken): Directive[Tuple1[Claims]] = {
    Directive[Tuple1[Claims]] { _ => //inner is ignored
      Directives.extractUri { uri =>
        Directives.redirect(loginUri(uri), StatusCodes.TemporaryRedirect)
      }
    }
  }

  private def extractCredentials: HttpHeader => Option[String] = {
    case Authorization(OAuth2BearerToken(jwt)) => Some(jwt)
    case _                                     => None
  }

}
