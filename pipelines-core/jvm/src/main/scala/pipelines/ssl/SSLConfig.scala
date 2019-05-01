package pipelines.ssl

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.security.{KeyStore, SecureRandom}

import com.typesafe.config.Config
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import scala.util.Try

/**
  *
  * @see see https://docs.oracle.com/javase/9/docs/specs/security/standard-names.html for keystore types
  * @param pathToCertificate the path to the keystore file
  * @param keystoreType the keystore type (e.g. pkcs12, jks, etc)
  * @param keystorePw the keystore pw
  * @param serverTlsSeed the keystore pw
  */
class SSLConfig private (pathToCertificate: Path, val keystoreType: String, keystorePw: String, serverTlsSeed: String) {

  def newContext(): Try[SSLContext] = {
    withP12Certificate {
      case (ksPwd, seed, is) => SSLConfig.sslContext(ksPwd, seed, is, keystoreType)
    }
  }

  def withP12Certificate[T](cert: (Array[Char], Array[Byte], InputStream) => T): Try[T] = {
    val is = Files.newInputStream(pathToCertificate)
    try {
      SSLConfig.withArray[Char, Try[T]](keystorePw.toCharArray(), ' ') { ksPassword =>
        SSLConfig.withArray[Byte, Try[T]](serverTlsSeed.getBytes(StandardCharsets.UTF_8), 0: Byte) { seed =>
          Try(cert(ksPassword, seed, is))
        }
      }
    } finally {
      is.close()
    }
  }
}

object SSLConfig {
  import eie.io._

  def fromRootConfig(rootConfig: Config): SSLConfig = apply(rootConfig.getConfig("pipelines.tls"))

  def apply(config: Config): SSLConfig = {
    apply(
      certPathOpt(config).getOrElse(throw new IllegalStateException(s"'certificate' set to '${certPathString(config)}' does not exist")),
      config.getString("password"),
      tlsSeedOpt(config).getOrElse(throw new IllegalStateException(s"'pipelines.tls.seed' not set"))
    )
  }

  def sslContext(password: Array[Char], seed: Array[Byte], keystore: InputStream, ksType: String): SSLContext = {
    sslContext(password, new SecureRandom(seed), keystore, ksType)
  }

  def sslContext(password: Array[Char], random: SecureRandom, keystore: InputStream, keyStoreType: String): SSLContext = {
    val ks: KeyStore = KeyStore.getInstance(keyStoreType)
    ks.load(keystore, password)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val ctxt: SSLContext = SSLContext.getInstance("TLS")
    ctxt.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, random)
    ctxt
  }

  def tlsSeed(config: Config)                   = config.getString("seed")
  def tlsSeedOpt(config: Config)                = Option(tlsSeed(config)).filterNot(_.isEmpty)
  def certPathString(config: Config)            = config.getString("certificate")
  def certPathOpt(config: Config): Option[Path] = Option(certPathString(config)).map(_.asPath).filter(_.isFile)

  def apply(pathToCertificate: Path, keystorePW: String, serverTlsSeed: String): SSLConfig = {
    val ksType = pathToCertificate.fileName match {
      case P12() => "pkcs12"
      case JKS() => "jks"
      case other => sys.error(s"Unable to determine the key store type from ${other}")
    }
    apply(pathToCertificate, ksType, keystorePW, serverTlsSeed)
  }

  def apply(pathToCertificate: Path, keystoreType: String, pw: String, serverTlsSeed: String): SSLConfig = {
    new SSLConfig(pathToCertificate, keystoreType, pw, serverTlsSeed)
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
