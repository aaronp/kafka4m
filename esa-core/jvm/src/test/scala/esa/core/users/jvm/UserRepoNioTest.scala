package esa.core.users.jvm
import org.scalatest.{Matchers, WordSpec}

class UserRepoNioTest extends WordSpec with Matchers {

  "UserRepoNio" should {
    "find users" in {
      UserLocalFileRepo()
    }
  }
}
