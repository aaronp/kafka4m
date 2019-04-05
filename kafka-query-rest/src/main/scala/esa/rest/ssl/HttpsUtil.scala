package esa.rest.ssl
import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsUtil {

  def apply(password: Array[Char], seed: Array[Byte], keystore: InputStream, ksType: String): HttpsConnectionContext = {
    apply(password, new SecureRandom(seed), keystore, ksType)
  }

  def apply(password: Array[Char], random: SecureRandom, keystore: InputStream, keyStoreType: String): HttpsConnectionContext = {
    val ks: KeyStore = KeyStore.getInstance(keyStoreType)
    require(keystore != null, "Null keystore")
    ks.load(keystore, password)

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslCtxt = SSLContext.getInstance("TLS")
    sslCtxt.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, random)

    ConnectionContext.https(sslCtxt)
  }
}
