package kafka4m.producer

import kafka4m.BaseKafka4mSpec
import org.apache.kafka.clients.producer.ProducerRecord

class AsProducerRecordTest extends BaseKafka4mSpec {
  "AsProducerRecord.contramap" should {
    "convert types" in {
      val asRecord: AsProducerRecord[String] = AsProducerRecord.liftForTopic[String]("foo") { text =>
        (text.reverse, text.getBytes)
      }
      val intAsRecord = asRecord.contraMap[Int](_.toString)
      val actual      = intAsRecord.asRecord(123)
      actual.key shouldBe "321"
      actual.value shouldBe "123".getBytes()
      actual.topic shouldBe "foo"
    }
  }
  "AsProducerRecord.liftForTopic" should {
    "produce records" in {
      val asRecord: AsProducerRecord[String] = AsProducerRecord.liftForTopic[String]("foo") { text =>
        (text.reverse, text.getBytes)
      }
      val actual: ProducerRecord[asRecord.K, asRecord.V] = asRecord.asRecord("hello")
      actual.key shouldBe "olleh"
      actual.value shouldBe "hello".getBytes()
      actual.topic shouldBe "foo"
    }
  }
}
