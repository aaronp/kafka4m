package kafka4m.consumer

import kafka4m.{Bytes, Key}
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
  * A means of decoding kafka records
  * @tparam K the key type
  * @tparam A the value type
  * @tparam R the result type
  */
trait RecordDecoder[K, A, R] { self =>
  def decode(record: ConsumerRecord[K, A]): R

}
object RecordDecoder {

  type ByteArrayDecoder[A] = RecordDecoder[kafka4m.Key, kafka4m.Bytes, A]

  def apply[K, A, R](implicit instance: RecordDecoder[K, A, R]): RecordDecoder[K, A, R] = instance

  def ByteArrayDecoder[A](implicit instance: ByteArrayDecoder[A]): ByteArrayDecoder[A] = instance

  implicit def fromBytesDecoder[A](implicit fromBytes: BytesDecoder[A]): RecordDecoder[Key, Bytes, A] = new ByteArrayDecoder[A] {
    override def decode(record: ConsumerRecord[Key, Bytes]) = fromBytes.decode(record.value())
  }

  implicit def identity[K, A]: RecordDecoder[K, A, ConsumerRecord[K, A]] = new RecordDecoder[K, A, ConsumerRecord[K, A]] {
    override def decode(record: ConsumerRecord[K, A]) = record
  }
}
