package kafkaquery.rest

import java.util.UUID

import org.scalatest.{Matchers, WordSpec}

class GenCertsTest extends WordSpec with Matchers {

  "GenCerts" should {
    "create a certificate" in {
      import eie.io._
      val dir          = s"target/certTest-${UUID.randomUUID}".asPath.mkDirs()
      val testHostName = "testHostName"

      try {
        val pwd                  = "password"
        val (res, log, certFile) = GenCerts.genCert(dir, "cert.p12", testHostName, pwd, pwd, pwd)
        res shouldBe 0
        withClue(log.allOutput) {
          certFile.size.toInt should be > 0
          log.allOutput should include(certFile.fileName)
        }
      } finally {
        dir.delete(true)
      }
    }
  }
}
