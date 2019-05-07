package pipelines.expressions

import io.circe.Json
import io.circe.optics.JsonPath

class RichJsonPath(val path: JsonPath) extends AnyVal {

  import implicits._
  def =====(other: JsonPath)(implicit json: Json): Boolean = {
    // TODO: other types
    path.number.=====(other.number) ||
    path.string.=====(other.string)
  }

  def asIntOpt(implicit json: Json): Option[Int] = path.number.getOption(json).flatMap(_.toInt)
  def asInt(implicit json: Json): Int            = asIntOpt.getOrElse(Int.MinValue)

  def asLongOpt(implicit json: Json): Option[Long] = path.number.getOption(json).flatMap(_.toLong)
  def asLong(implicit json: Json): Long            = asLongOpt.getOrElse(Long.MinValue)

  def asDoubleOpt(implicit json: Json): Option[Double] = path.number.getOption(json).map(_.toDouble)
  def asDouble(implicit json: Json): Double            = asDoubleOpt.getOrElse(Double.MinValue)

  def asBooleanOpt(implicit json: Json): Option[Boolean] = path.boolean.getOption(json)
  def asBoolean(implicit json: Json): Boolean            = asBooleanOpt.getOrElse(false)

  def asStringOpt(implicit json: Json): Option[String] = path.string.getOption(json)
  def asString(implicit json: Json): String            = asStringOpt.getOrElse("")

  def =====(value: String)(implicit json: Json): Boolean = path.string.=====(value)
  def =====(value: Double)(implicit json: Json): Boolean = path.number.=====(value)
  def =====(value: Long)(implicit json: Json): Boolean   = path.string.=====(value)
  def <=(value: Double)(implicit json: Json): Boolean    = path.number.<=(value)
  def >=(value: Double)(implicit json: Json): Boolean    = path.number.>=(value)
  def >(value: Double)(implicit json: Json): Boolean     = path.number.>(value)
  def <(value: Double)(implicit json: Json): Boolean     = path.number.<(value)

}
