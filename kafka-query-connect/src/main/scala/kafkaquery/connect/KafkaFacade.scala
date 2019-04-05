package kafkaquery.connect

import kafkaquery.kafka.{ListTopicsResponse, PullLatestResponse}

import scala.concurrent.duration.FiniteDuration

trait KafkaFacade {
  def listTopics(): ListTopicsResponse
  def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse
}

object KafkaFacade {

  def apply(kafka: RichKafkaConsumer[String, Bytes], pollTimeout: FiniteDuration, timeout: FiniteDuration): KafkaFacade = new KafkaFacade {
    override def listTopics() = {
      ListTopicsResponse(kafka.listTopics())
    }

    override def pullLatest(topic: String, offset: Long, limit: Long) = {
      kafka.pullLatest(topic, offset, limit, pollTimeout, timeout, identity)
    }
  }
}
