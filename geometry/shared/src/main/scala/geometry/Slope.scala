package geometry

sealed trait Slope {
  def isVertical: Boolean   = value.isInfinite
  def isHorizontal: Boolean = value == 0.0
  def value: Double
  def reciprocal: Slope
  def orthogonal: Slope
  def -(other: Slope): Slope = {
    DoubleSlope(value - other.value)
  }
}

case class DoubleSlope(override val value: Double) extends Slope {
  override def reciprocal: Slope = DoubleSlope(1.0 / value)
  override def orthogonal: Slope = DoubleSlope(1.0 / -value)
  override def toString          = value.toString
}

case class Fraction(rise: Int, run: Int) extends Slope {
  override def toString = s"$rise/$run"
  override def -(otherSlope: Slope): Slope = {
    otherSlope match {
      case other: Fraction =>
        val gcm  = run.abs * other.run.abs
        val num1 = rise * (gcm / run)
        val num2 = other.rise * (gcm / other.run)
        Fraction(num1 - num2, gcm)
      case other => DoubleSlope(value - other.value)
    }
  }
  def +(other: Fraction) = {
    val gcm  = run * other.run
    val num1 = rise * gcm
    val num2 = other.rise * gcm
    Fraction(num1 + num2, gcm).reduce
  }

  def reduce: Fraction = {
    if (run < 0) {
      Fraction(-rise, -run).reduce
    } else {
      val m = gcd(rise.abs, run.abs)
      if (m == 1) {
        this
      } else {
        Fraction(rise / m, run / m)
      }
    }
  }
  override def reciprocal: Slope = Fraction(run, rise).reduce
  override def orthogonal: Slope = Fraction(-run, rise).reduce

  lazy val value: Double = {
    if (run == 0.0) {
      Double.PositiveInfinity
    } else {
      rise.toDouble / run.toDouble
    }
  }
}

object Slope {
  private val StrR                = "-?(\\d)\\.(\\d+)".r
  def apply(num: Int, denom: Int) = Fraction(num, denom)

  def apply(from: Double): Slope = {
    if (from.isNaN || from.isInfinity) {
      DoubleSlope(Double.PositiveInfinity)
    } else {

      val digits = from.toString match {
        case StrR(_, denomStr) => denomStr.length
        case _                 => 2
      }

      val possibilities = Iterator.from(digits).map { n =>
        val scale: Double = 10 * n
        val numerator     = from * scale
        Fraction(numerator.toInt, scale.toInt)
      }

      val s = possibilities.toStream.take(10)
      val opt = s.find { p =>
        val diff = from - p.value
        diff == 0.0
      }
      opt.map(_.reduce).getOrElse(DoubleSlope(from))
    }
  }
}
