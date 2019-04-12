package pipelines.connect

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}

import org.apache.kafka.clients.consumer.ConsumerRecord

case class RecordJson(topic: String, partition: Int, offset: Long, key: String, value: String, timestamp: Long, timestampType: String, timestampText: String)

object RecordJson {
  import io.circe.generic.semiauto._

  implicit val encoder = deriveEncoder[RecordJson]
  implicit val decoder = deriveDecoder[RecordJson]

  def apply(record: ConsumerRecord[String, String]): RecordJson = {
    apply(record, record.key, record.value)
  }

  def apply(record: ConsumerRecord[_, _], key: String, value: String): RecordJson = {
    val time = {
      val localTime = LocalDateTime.ofEpochSecond(record.timestamp, 0, ZoneOffset.UTC)
      DateTimeFormatter.ISO_DATE_TIME.format(localTime)
    }

    new RecordJson(
      topic = record.topic,
      partition = record.partition,
      offset = record.offset,
      key = key,
      value = value,
      timestamp = record.timestamp,
      timestampType = record.timestampType.name,
      timestampText = time
    )
  }
}
