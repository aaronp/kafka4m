package kafkaquery.kafka
import io.circe.Decoder.Result
import io.circe.{DecodingFailure, HCursor, Json}

import scala.collection.immutable

sealed trait KafkaRequest
sealed trait KafkaResponse

final case class PartitionData(partition: Int, leader: String)

final case class ListTopicsResponse(topics: Map[String, Seq[PartitionData]]) extends KafkaResponse

final case class PullLatestResponse(topic: String, keys: Seq[String]) extends KafkaResponse

/**
  * How should a subscriber consume the data?
  *
  * e.g., consider a subscriber who wants 100 data points/second, but only requests more elements at a rate of 10/second?
  *
  * Should we send each second (e.g. the first second's worth of 100 data points will take 10 seconds. On the 11th second, should
  * we send the subsequent second's 100 data points, or jump to the 'latest')?
  *
  */
sealed class StreamStrategy(val name: String)
final case object Latest extends StreamStrategy("latest")
final case object All    extends StreamStrategy("all")

object StreamStrategy {
  lazy val values: List[StreamStrategy] = List(Latest, All)
  implicit object StrategyEncoder extends io.circe.Encoder[StreamStrategy] {
    override def apply(a: StreamStrategy): Json = {
      Json.fromString(a.name)
    }
  }
  implicit object StrategyDecoder extends io.circe.Decoder[StreamStrategy] {
    override def apply(c: HCursor): Result[StreamStrategy] = {
      c.as[String].flatMap { name =>
        values.find(_.name == name) match {
          case Some(value) => Right(value)
          case None        => Left(DecodingFailure(s"Expected one of ${values.map(_.name).mkString(",")} but got $name", c.history))
        }
      }
    }
  }
}

final case class StreamRequest(clientId: String,
                               groupId: String,
                               topic: String,
                               filterExpression: String,
                               fromOffset: Option[Long],
                               messageLimitPerSecond: Option[Int],
                               streamStrategy: StreamStrategy)
    extends KafkaRequest

sealed trait KafkaSupportRequest
final case class PublishMessage(topic: String, key: String, data: String) extends KafkaSupportRequest
