package esa.core.users

/**
  * See https://stackoverflow.com/questions/13912597/validate-email-one-liner-in-scala
  *
  */
object EmailValidation {

  val EmailMaxLen = 100
  private val NameAndDomainR = """(\w+)@([\w\.]+)""".r
  private val PartR = """(?=[^\s]+)(?=(\w+)@([\w\.]+))""".r

  private def isValidPart(email: String): Boolean = PartR.findFirstIn(email).isDefined

  def isValid(email: String): Boolean = email match {
    case NameAndDomainR(name, domain) => isValidPart(name) && isValidPart(domain)
    case _ => false
  }
}
