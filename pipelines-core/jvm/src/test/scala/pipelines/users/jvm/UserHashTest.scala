package pipelines.users.jvm

import org.scalatest.{Matchers, WordSpec}

class UserHashTest extends WordSpec with Matchers {

  "UserHash" should {
    "hash consistently" in {
      val hasher = UserHash("secret".getBytes("UTF-8"))
      val hash   = hasher("P4ssw0rd")
      hash should not be "P4ssw0rd"
      hash should not be hasher("P4ssw0rd2")
      hash should not be hasher("P4ssw0rd".reverse)
    }
  }
}
