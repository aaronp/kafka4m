package pipelines.rest.jwt
import java.time.ZonedDateTime
import java.util
import java.util.Base64

import io.circe
import javax.crypto.spec.SecretKeySpec

/**
  * Represents the parts of a Json Web Token. The signature is just the data - IT MAY NOT BE VALID!
  *
  *
  * Use 'isValidForSecret' and 'isHs256' to ensure this is a valid token
  *
  * @param header
  * @param claims
  * @param signature
  */
class JsonWebToken(val header: JwtHeader, val claims: Claims, val signature: String) {
  override lazy val hashCode: Int = {
    util.Arrays.hashCode(Array(header.hashCode(), claims.hashCode(), signature.hashCode()))
  }

  def resetExpiry(newExpiry: ZonedDateTime, secret: SecretKeySpec): JsonWebToken = {
    require(isHs256, "not an HS256 token")
    val newClaims    = claims.copy(exp = Claims.asNumericDate(newExpiry))
    val newPayload   = s"${header.toJsonBase64}.${newClaims.toJsonBase64}"
    val newSignature = JsonWebToken.hmac256SignPayload(newPayload, secret)
    new JsonWebToken(header, newClaims, newSignature)
  }

  def isHs256 = header == JwtHeader.hs256

  private def payload = s"${header.toJsonBase64}.${claims.toJsonBase64}"

  def isValidForSecret(secret: String): Boolean        = validateSignature(Hmac256(secret, payload))
  def isValidForSecret(secret: SecretKeySpec): Boolean = validateSignature(Hmac256(secret, payload))

  private def validateSignature(secret: Array[Byte]): Boolean = {
    val encoded = Base64.getUrlEncoder.encode(secret)
    util.Arrays.equals(encoded, signature.getBytes("UTF-8"))
  }

  override def toString = asToken
  lazy val asToken      = s"$payload.$signature"

  override def equals(other: Any) = {
    other match {
      case jwt: JsonWebToken =>
        jwt.header == header &&   //
          jwt.claims == claims && //
          jwt.signature == signature
      case _ => false
    }
  }
}

object JsonWebToken {
  private val JwtR = """(.*)\.(.*)\.(.*)""".r

  sealed trait JwtError                  extends Exception
  case object CorruptJwtSecret           extends JwtError
  case object UnsupportedAlgo            extends JwtError
  case object InvalidJwtFormat           extends JwtError
  case class UnmarshalError(str: String) extends JwtError

  def asHmac256Token(claims: Claims, secret: String): String = asHmac256Token(claims, Hmac256.asSecret(secret))

  def hmac256SignPayload(payload: String, secret: SecretKeySpec) = {
    val encoded: Array[Byte] = Hmac256(secret, payload)
    Base64.getUrlEncoder.encodeToString(encoded)
  }

  def asHmac256Token(claims: Claims, secret: SecretKeySpec): String = {
    val payload   = s"${JwtHeader.hs256JsonBase64}.${claims.toJsonBase64}"
    val signature = hmac256SignPayload(payload, secret)
    s"${payload}.${signature}"
  }

  def asToken(headerJsonInBase64: String, claims: Claims, secret: SecretKeySpec): String = {
    val payload = s"${headerJsonInBase64}.${claims.toJsonBase64}"
    val signature = {
      val encoded: Array[Byte] = Hmac256(secret, payload)
      new String(encoded, "UTF-8")
    }
    s"${payload}.${signature}"
  }

  private def fromBase64(base64Str: String) = {
    new String(Base64.getUrlDecoder.decode(base64Str), "UTF-8")
  }

  def forToken(jwt: String, secret: SecretKeySpec): Either[JwtError, JsonWebToken] = {
    parseToken(jwt).right.flatMap {
      case token if !token.isHs256 => Left(UnsupportedAlgo)
      case token =>
        if (token.isValidForSecret(secret)) {
          Right(token)
        } else {
          Left(CorruptJwtSecret)
        }
    }
  }

  def parseToken(jwt: String): Either[JwtError, JsonWebToken] = {
    jwt match {
      case JwtR(headerB64, claimsB64, signature) =>
        val headerE: Either.RightProjection[circe.Error, JwtHeader] = JwtHeader.fromJson(fromBase64(headerB64)).right

        val headerAndClaims = headerE.flatMap { header =>
          val claimsE = Claims.fromJson(fromBase64(claimsB64)).right
          claimsE.map { c =>
            new JsonWebToken(header, c, signature)
          }
        }

        headerAndClaims.left.map(err => UnmarshalError(err.toString))
      case _ => Left(InvalidJwtFormat)
    }
  }
}
