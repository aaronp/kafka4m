package kafkaquery.connect

import kafkaquery.kafka.{ListTopicsResponse, PullLatestResponse, StreamRequest}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.Publisher

import scala.concurrent.duration.FiniteDuration

trait KafkaFacade {
  def listTopics(): ListTopicsResponse
  def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse

  def stream(request: StreamRequest): Publisher[ConsumerRecord[String, Bytes]]
}

object KafkaFacade {

  def apply(kafka: RichKafkaConsumer[String, Bytes], pollTimeout: FiniteDuration, timeout: FiniteDuration): KafkaFacade = new KafkaFacade {
    override def listTopics() = {
      ListTopicsResponse(kafka.listTopics())
    }

    override def pullLatest(topic: String, offset: Long, limit: Long) = {
      kafka.pullLatest(topic, limit, pollTimeout, timeout, identity)
    }

    override def stream(request: StreamRequest): Publisher[ConsumerRecord[String, Bytes]] = {
//      request.groupId
//      request.clientId
      ???
    }
  }
}
