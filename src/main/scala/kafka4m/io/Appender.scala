package kafka4m.io

/**
  * Represents a sink for writing values to some persistent source
  * @tparam A
  */
trait Appender[A] extends AutoCloseable {

  /**
    * Write some value
    * @param value
    */
  def append(value: A): Unit
}
