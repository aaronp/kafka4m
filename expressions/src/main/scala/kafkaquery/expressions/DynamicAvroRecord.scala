package kafkaquery

import org.apache.avro.specific.SpecificRecordBase

import scala.language.dynamics
import scala.util.Try

/**
  * TODO - we naively convert to a double here so that 'foo.speed > 100' type-checks w/ the '>'.
  *
  * Really the field could be anything, so we'll probably want to (1) check the parsed AST to see if the next
  * operator is a numeric one (otherwise we don't need to do this cast, as we could just use the object equality
  * if the expression is 'foo.field == "some value"')
  *
  * Alternatively we could return our own 'Value' class which supports a Numeric typeclass instance, which
  * would only error/throw when it wrapped a non-numeric type AND it was tried to be used as a comparison
  */
class DynamicAvroRecord(val msg: SpecificRecordBase) extends AnyVal with Dynamic {

  def selectDynamic(fieldName: String): DynamicAvroRecord.Value = {
    val schema = msg.getSchema

    val field = {
      val fld = schema.getField(fieldName)
      if (fld == null && !fieldName.startsWith("get")) {
        val (head, tail) = fieldName.splitAt(1)
        val getter       = s"get${head.capitalize}$tail"
        schema.getField(getter)
      } else {
        fld
      }
    }
    DynamicAvroRecord.Value(msg.get(field.pos))
  }
}

object DynamicAvroRecord {

  object Value {

    def apply(x: Int) = {
      new Value(java.lang.Integer.valueOf(x))
    }

    def apply(x: Double) = {
      new Value(java.lang.Double.valueOf(x))
    }

    implicit def coerceToDouble(value: Value) = value.asDouble
  }

  case class Value(actual: AnyRef) extends Comparable[Value] {
    lazy val asDouble: Double = actual match {
      case x: java.lang.Double  => x
      case x: java.lang.Float   => x.toDouble
      case x: java.lang.Integer => x.toDouble
      case x: java.lang.Long    => x.toDouble
      case x: BigInt            => x.toDouble
      case x: BigDecimal        => x.toDouble
    }

    /**
      * The 'asDouble' will be the typical use case, so instead of wrapping/unwrapping an option,
      * we prefer to just throw for the common case, and thus try/catch that error to provide the optional
      * case
      *
      * @return
      */
    def asDoubleOpt: Option[Double] = {
      Try(asDouble).toOption
    }

    def asStringOpt: Option[String] = {
      actual match {
        case s: String => Option(s)
        case _         => None
      }
    }

    lazy val asInt: Int = actual match {
      case x: java.lang.Integer => x
      case x: java.lang.Double  => x.toInt
      case x: java.lang.Float   => x.toInt
      case x: java.lang.Long    => x.toInt
      case x: BigInt            => x.toInt
      case x: BigDecimal        => x.toInt
    }
    lazy val asLong: Long = actual match {
      case x: java.lang.Integer => x.toLong
      case x: java.lang.Double  => x.toLong
      case x: java.lang.Float   => x.toLong
      case x: java.lang.Long    => x
      case x: BigInt            => x.toLong
      case x: BigDecimal        => x.toLong
    }
    lazy val asString: String = actual match {
      case x: String => x
    }

    override def compareTo(other: Value): Int = {
      (asDoubleOpt, other.asDoubleOpt) match {
        case (Some(lhs), Some(rhs)) => lhs.compareTo(rhs)
        case _ =>
          (asStringOpt, other.asStringOpt) match {
            case (Some(lhs), Some(rhs)) => lhs.compareTo(rhs)
            case _                      => toString.compareTo(other.toString)
          }
      }
    }
  }

  implicit object ValueNumeric extends Numeric[Value] {
    def doubleNumeric = implicitly[Numeric[Double]]

    private def valueFromDouble(result: Double): Value = Value(result)

    private def valueFromInt(result: Int): Value = Value(result)

    override def plus(x: Value, y: Value): Value = valueFromDouble(x.asDouble + y.asDouble)

    override def minus(x: Value, y: Value): Value = valueFromDouble(x.asDouble - y.asDouble)

    override def times(x: Value, y: Value): Value = valueFromDouble(x.asDouble * y.asDouble)

    override def negate(x: Value): Value = valueFromDouble(doubleNumeric.negate(x.asDouble))

    override def fromInt(x: Int): Value = valueFromInt(x)

    override def toInt(x: Value): Int = x.asInt

    override def toLong(x: Value): Long = x.asLong

    override def toFloat(x: Value): Float = x.asDouble.toFloat

    override def toDouble(x: Value): Double = x.asDouble

    override def compare(x: Value, y: Value): Int = {
      x.compareTo(y)
    }
  }

}
