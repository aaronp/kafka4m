package kafkaquery

import java.awt.Desktop
import java.net.URI
import java.nio.file.Path

import com.typesafe.scalalogging.StrictLogging
import kafkaquery.connect.DockerEnv
import kafkaquery.rest.ssl.GenCerts

import scala.io.StdIn

/**
  * This entry-point puts the test resources on the classpath, and so serves as a convenience for running up the Main entry-point for local development work.
  *
  */
object DevMain extends StrictLogging {

  /**
    * Create our own self-signed cert (if required) for local development
    */
  def ensureCert() = {
    import eie.io._
    val certFile = "target/certificates/cert.p12".asPath
    if (!certFile.isFile) {
      logger.info(s"${certFile} doesn't exist, creating it")
      val pwd                          = "password"
      val (resValue, buffer, certPath) = GenCerts.genCert(certFile.getParent, certFile.fileName, "", pwd, pwd, pwd)
      logger.info(s"created ${certPath}:\n\n${buffer.allOutput}\n\n")
      require(resValue == 0)
    } else {
      logger.info("dev cert exists, cracking on...")
    }
  }

  def main(a: Array[String]): Unit = {
    ensureCert()
    DockerEnv("scripts/kafka").bracket {
      rest.Main.main(a :+ "dev.conf")
      lazy val dt = Desktop.getDesktop

      if (Desktop.isDesktopSupported && dt.isSupported(Desktop.Action.BROWSE)) {
        dt.browse(new URI("https://localhost:80"))
      }
      StdIn.readLine("Running dev main - hit any key to stop...")
    }
    println("Goodbye!")
    sys.exit(0)
  }
}
