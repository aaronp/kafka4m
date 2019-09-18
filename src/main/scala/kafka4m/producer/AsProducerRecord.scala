package kafka4m.producer

import kafka4m.{Bytes, Key, KeyValue}
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

  final def contraMap[B](f: B => A): AsProducerRecord[B] = {
    val parent = this
    new AsProducerRecord[B] {
      override type K = parent.K
      override type V = parent.V
      override def asRecord(value: B): ProducerRecord[K, V] = {
        parent.asRecord(f(value))
      }
    }
  }
}

object AsProducerRecord {
  type Aux[A, Key, Value] = AsProducerRecord[A] {
    type K = Key
    type V = Value
  }

  def apply[A](implicit apr: AsProducerRecord[A]): AsProducerRecord[A] = apr

  def lift[A, KafkaKey, KafkaValue](f: A => ProducerRecord[KafkaKey, KafkaValue]): AsProducerRecord[A] = new AsProducerRecord[A] {
    override type K = KafkaKey
    override type V = KafkaValue
    override def asRecord(value: A) = f(value)
  }

  def liftForTopic[A](topic: String)(f: A => KeyValue): AsProducerRecord[A] = {
    lift[A, Key, Bytes] { a =>
      val (key, data) = f(a)
      new ProducerRecord[Key, Bytes](topic, key, data)
    }
  }

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
