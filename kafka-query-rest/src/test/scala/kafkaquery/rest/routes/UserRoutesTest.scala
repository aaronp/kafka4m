package kafkaquery.rest.routes
import java.time.{ZoneId, ZonedDateTime}

import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import kafkaquery.rest.jwt.JsonWebToken.CorruptJwtSecret
import kafkaquery.rest.jwt.{Claims, Hmac256, JsonWebToken}
import io.circe.generic.auto._
import kafkaquery.users.{LoginRequest, LoginResponse}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class UserRoutesTest extends WordSpec with Matchers with ScalatestRouteTest {

  "UserRoutes.route" should {
    // pass in a fixed 'now' time when the admin user logs in for this test
    val loginTime = ZonedDateTime.of(2019, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"))

    val adminClaims = Claims.after(5.minutes, loginTime).forUser("admin")
    val loginRoute = UserRoutes("server secret") {
      case LoginRequest("admin", "password") => Option(adminClaims)
      case _                                 => None
    }

    "reject invalid logins" in {
      Post("/login", LoginRequest("admin", "bad password")) ~> loginRoute.loginRoute ~> check {
        val LoginResponse(false, None, None) = responseAs[LoginResponse]
        response.headers.find(_.lowercaseName() == "x-access-token").map(_.value()) should be(empty)
      }
      Post("/login", LoginRequest("guest", "password")) ~> loginRoute.loginRoute ~> check {
        val LoginResponse(false, None, None) = responseAs[LoginResponse]
        response.headers.find(_.lowercaseName() == "x-access-token").map(_.value()) should be(empty)
      }
    }
    "redirect successful logins the user was redirected from another attempted page" in {
      val loginRequest: HttpRequest = {
        val req = Post("/login", LoginRequest("admin", "password"))
        req.withUri(req.uri.withRawQueryString("redirectTo=/foo/bar"))
      }
      loginRequest ~> loginRoute.loginRoute ~> check {

        header[Location].get.uri.path.toString() shouldBe "/foo/bar"
        val List(xAccessToken) = header("x-access-token").map(_.value()).toList
        xAccessToken should not be (empty)

        val Right(jwt) = JsonWebToken.forToken(xAccessToken, Hmac256.asSecret("server secret"))
        jwt.claims shouldBe adminClaims

        jwt.asToken shouldBe xAccessToken

        status shouldBe StatusCodes.TemporaryRedirect
      }
    }
    "be able to login successfully and return a jwt token" in {
      Post("/login", LoginRequest("admin", "password")) ~> loginRoute.loginRoute ~> check {
        val LoginResponse(true, Some(token), None) = responseAs[LoginResponse]

        val List(xAccessToken) = header("x-access-token").map(_.value()).toList
        xAccessToken shouldBe token

        val Right(jwt) = JsonWebToken.forToken(xAccessToken, Hmac256.asSecret("server secret"))
        jwt.claims shouldBe adminClaims

        JsonWebToken.forToken(xAccessToken, Hmac256.asSecret("corrupt secret")) shouldBe Left(CorruptJwtSecret)
      }
    }
  }
}
