package pipelines.expressions

/**
  * Adds the '=====' function to any type.
  *
  * This is done in order to support json expressions where we want to override '==', so we turn that text into '=====', but
  * then need to add '=====' to any other type.
  *
  * Ideally we don't do all this string hacking and just implement a macro
  *
  * @param any
  */
class RichType(val any: Any) extends AnyVal {

  def =====(other: Any) = any == other
}
