package kafkaquery.core.users.jvm
import java.security.SecureRandom
import java.util.Base64

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserHash private[jvm] (random: SecureRandom, salt: Array[Byte], iterationCount: Int, keyLen: Int) {

  private val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
  private val enc = Base64.getEncoder

  def apply(password: String): String = {
    val spec = new PBEKeySpec(password.toCharArray, salt, iterationCount, keyLen)
    enc.encodeToString(skf.generateSecret(spec).getEncoded)
  }
}

object UserHash {

  def apply(salt: Array[Byte], iterationCount: Int = 65536, keyLen: Int = 128): UserHash = {
    val random = new SecureRandom(salt)
    new UserHash(random, salt.toList.toArray, iterationCount, keyLen)
  }
}
