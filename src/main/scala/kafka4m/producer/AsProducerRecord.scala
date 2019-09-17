package kafka4m.producer

import kafka4m.{Bytes, Key}
import org.apache.kafka.clients.producer.ProducerRecord

/**
  * A typeclass to allow the publication of any record 'A' which can be converted into a producer record
  *
  * @tparam A
  */
trait AsProducerRecord[-A] {

  /** The key type */
  type K

  /** The value type */
  type V

  /** @param value the value to convert
    * @return a producer record for the given value
    */
  def asRecord(value: A): ProducerRecord[K, V]
}

object AsProducerRecord {
  type Aux[A, Key, Value] = AsProducerRecord[A] {
    type K = Key
    type V = Value
  }

  def apply[A](implicit apr: AsProducerRecord[A]): AsProducerRecord[A] = apr

  case class FromString(topic: String) extends AsProducerRecord[String] {
    override type K = Key
    override type V = Bytes

    override def asRecord(value: String) = {
      new ProducerRecord[K, V](topic, value.getBytes("UTF-8"))
    }
  }

  case class FromKeyAndBytes(topic: String) extends AsProducerRecord[(String, Array[Byte])] {
    override type K = Key
    override type V = Bytes

    override def asRecord(value: (String, Array[Byte])): ProducerRecord[Key, Bytes] = {
      val (key, data) = value
      new ProducerRecord[Key, Array[Byte]](topic, key, data)
    }
  }

}
