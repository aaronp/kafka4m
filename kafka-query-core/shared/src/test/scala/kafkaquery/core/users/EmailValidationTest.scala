package kafkaquery.core.users
import org.scalatest.{Matchers, WordSpec}

class EmailValidationTest extends WordSpec with Matchers {

  "EmailValidation" ignore {

    List(
      "aaron.p@gmail.com",
      "aaron_p@gmail.com",
      "aaron-p@gmail.com",
      "aaron$@gmail.com",
      "ALL@CAPS.ORG",
      "ALL@CAPS",
      "123@678.com",
      "Jaimes123@company.org.uk",
      "a@example.foo"
    ).foreach { validEmail =>
      s"return true for $validEmail" in {
        EmailValidation.isValid(validEmail) shouldBe true
      }
    }

    List(
      ("a" * EmailValidation.EmailMaxLen + "@gmail.com "),
      "",
      "a",
      "foo@",
      "foo@@bar.com",
      "foo@x@bar.com",
      "foo$!@x@bar.com"
    ).foreach { validEmail =>
      s"return false for $validEmail" in {
        EmailValidation.isValid(validEmail) shouldBe false
      }
    }
  }
}
