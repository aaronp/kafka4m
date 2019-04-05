package esa.rest.jwt

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Hmac256 {

  def apply(secret: String, payload: String): Array[Byte] = apply(asSecret(secret), payload)

  def apply(secretKeySpec: SecretKeySpec, payload: String): Array[Byte] = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKeySpec)
    mac.doFinal(payload.getBytes)
  }
  def asSecret(secret: String): SecretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "SHA256")

}
