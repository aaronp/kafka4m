package kafka4m.io

/**
  * Represents a sink for writing values to some persistent source
  * @tparam A
  */
trait Appender[A] extends AutoCloseable {
  def append(value: A): Unit
}

object Appender {}
