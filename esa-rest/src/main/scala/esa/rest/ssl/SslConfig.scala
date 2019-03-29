package esa.rest.ssl
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import com.typesafe.config.Config

import scala.util.Try

/**
  *
  * @see see https://docs.oracle.com/javase/9/docs/specs/security/standard-names.html for keystore types
  * @param pathToCertificate the path to the keystore file
  * @param keystoreType the keystore type (e.g. pkcs12, jks, etc)
  * @param keystorePw the keystore pw
  * @param serverTlsSeed the keystore pw
  */
class SslConfig private (pathToCertificate: Path, val keystoreType: String, keystorePw: String, serverTlsSeed: String) {

  def withP12Certificate[T](cert: (Array[Char], Array[Byte], InputStream) => T): Try[T] = {
    val is = Files.newInputStream(pathToCertificate)
    try {
      SslConfig.withArray[Char, Try[T]](keystorePw.toCharArray(), ' ') { ksPassword =>
        SslConfig.withArray[Byte, Try[T]](serverTlsSeed.getBytes(StandardCharsets.UTF_8), 0: Byte) { seed =>
          Try(cert(ksPassword, seed, is))
        }
      }
    } finally {
      is.close()
    }
  }
}

object SslConfig {
  import eie.io._

  def apply(config: Config): SslConfig = {
    apply(
      config.getString("certificate").asPath.ensuring(_.isFile),
      config.getString("password"),
      config.getString("tls_seed"),
    )
  }

  def apply(pathToCertificate: Path, keystorePW: String, serverTlsSeed: String): SslConfig = {
    val ksType = pathToCertificate.fileName match {
      case P12() => "pkcs12"
      case JKS() => "jks"
      case other => sys.error(s"Unable to determine the key store type from ${pathToCertificate}")
    }
    apply(pathToCertificate, ksType, keystorePW, serverTlsSeed)
  }

  def apply(pathToCertificate: Path, keystoreType: String, pw: String, serverTlsSeed: String): SslConfig = {
    new SslConfig(pathToCertificate, keystoreType, pw, serverTlsSeed)
  }

  private object JKS {
    def unapply(path: String): Boolean = path.toLowerCase.endsWith(".jks")
  }
  private object P12 {

    def unapply(path: String): Boolean = {
      val lc = path.toLowerCase
      lc.endsWith(".p12") || lc.endsWith(".pfx")
    }
  }

  private def withArray[A, T](array: Array[A], clear: A)(f: Array[A] => T): T = {
    val result = f(array)
    array.indices.foreach(i => array(i) = clear)
    result
  }

}
