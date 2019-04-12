package pipelines.rest

import org.scalatest.{Matchers, WordSpec}

class MainTest extends WordSpec with Matchers {
  "Main.ensureCert" should {
    "create local certificates -- this test is also required for docker deploy to use in application.conf" in {
      val file = Main.ensureCert("target/certificates/cert.p12")
      import eie.io._
      file.isFile shouldBe true
    }
  }
}
