package kafkaquery.rest.jwt
import java.time.{ZoneId, ZonedDateTime}

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class ClaimsTest extends WordSpec with Matchers {

  "Claims.isExpired" should {
    "return true when a token expires" in {
      val now    = ZonedDateTime.of(2019, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"))
      val claims = Claims.after(10.seconds, now).forUser("somebody")
      claims.isExpired(now) shouldBe false
      claims.isExpired(now.plusSeconds(9)) shouldBe false
      claims.isExpired(now.plusSeconds(10)) shouldBe true
      claims.isExpired(now.plusSeconds(11)) shouldBe true

      Claims(name = "never expires").isExpired(now) shouldBe false
    }
  }
  "Claims json" should {
    "encode to and from json" in {
      val now  = ZonedDateTime.of(2019, 2, 3, 4, 5, 6, 6, ZoneId.of("UTC"))
      val bc   = Claims.after(10.seconds, now).forUser("Dave")
      val json = bc.toJson
      Claims.fromJson(json) shouldBe Right(bc)
    }
  }
}
