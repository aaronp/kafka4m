package kafkaquery.expresssions.ast

sealed trait Value

case class LongValue(value: Long) extends Value {

  def normalize(): Value = {
    if (value > 0) {
      if (value <= Int.MaxValue) {
        IntValue(value.toInt)
      } else {
        this
      }
    } else {
      if (value >= Int.MinValue) {
        IntValue(value.toInt)
      } else {
        this
      }
    }
  }
}

case class IntValue(value: Int) extends Value

case class DoubleValue(value: Double) extends Value

case class TextValue(value: String) extends Value

case class BooleanValue(value: Boolean) extends Value
