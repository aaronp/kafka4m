package pipelines

import java.awt.Desktop
import java.net.URI

import com.typesafe.scalalogging.StrictLogging
import pipelines.connect.DockerEnv

import scala.io.StdIn

/**
  * This entry-point puts the test resources on the classpath, and so serves as a convenience for running up the Main entry-point for local development work.
  *
  */
object DevMain extends StrictLogging {

  def main(a: Array[String]): Unit = {

    DockerEnv("scripts/kafka").bracket {
      val opt = rest.Main.runMain(a :+ "dev.conf" :+ "generateMissingCerts=true" :+ "pipelines.tls.certificate=target/certificates/cert.p12")
      if (opt.nonEmpty) {
        lazy val dt = Desktop.getDesktop
        if (Desktop.isDesktopSupported && dt.isSupported(Desktop.Action.BROWSE)) {
          dt.browse(new URI("https://localhost:80"))
        }
        StdIn.readLine("Running dev main - hit any key to stop...")
      }
    }
    println("Goodbye!")
    sys.exit(0)
  }
}
