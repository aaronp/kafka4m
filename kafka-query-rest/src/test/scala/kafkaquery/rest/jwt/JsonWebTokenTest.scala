package esa.rest.jwt
import org.scalatest.{Matchers, WordSpec}

class JsonWebTokenTest extends WordSpec with Matchers {

  "JsonWebToken" should {
    "create and validate an Hs256 token from claims and a secret" in {
      val token = JsonWebToken.asHmac256Token(Claims(name = "Alice"), "S3cr3t")
      token.count(_ == '.') shouldBe 2

      val Right(parsed) = JsonWebToken.parseToken(token)
      parsed.isValidForSecret("S3cr3t") shouldBe true
      parsed.isValidForSecret("S3cr3t!") shouldBe false
      parsed.isValidForSecret("wrong!") shouldBe false
      parsed.isHs256 shouldBe true
    }
  }
}
