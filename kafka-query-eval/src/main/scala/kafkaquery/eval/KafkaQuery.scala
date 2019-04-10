package kafkaquery.eval

import kafkaquery.connect.{Bytes, RichKafkaConsumer}
import kafkaquery.kafka.StreamRequest
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration.FiniteDuration

/**
  * Query a kafka topic using an expression
  */
object KafkaQuery {

  def apply(request: StreamRequest, timeout: FiniteDuration) = {

    val c: RichKafkaConsumer[String, Bytes]           = ???
    val data: Iterator[ConsumerRecord[String, Bytes]] = c.pull(timeout)

    val reader = AvroReader[GenericRecord]

    val predicate = kafkaquery.expressions.Expressions.cache(request.filterExpression)

    val mapped = data.map { record: ConsumerRecord[String, Bytes] =>
      record -> reader.read(record.value)
    }

    mapped.filter {
      case (_, d8a) => predicate(d8a)
    }

  }


}
