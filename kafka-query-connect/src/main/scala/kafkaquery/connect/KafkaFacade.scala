package kafkaquery.connect

import java.util.concurrent.ScheduledExecutorService

import kafkaquery.kafka.{ListTopicsResponse, PullLatestResponse}

import scala.concurrent.duration.FiniteDuration

trait KafkaFacade {
  def listTopics(): ListTopicsResponse
  def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse
}

object KafkaFacade {

  def apply(consumer: RichKafkaConsumer[String, Bytes], pollTimeout: FiniteDuration, timeout: FiniteDuration): KafkaFacade = new KafkaFacade {
    override def listTopics() = ListTopicsResponse(consumer.listTopics())
    override def pullLatest(topic: String, offset: Long, limit: Long) = {
      consumer.pullLatest(topic, limit, pollTimeout, timeout, identity)
    }
  }
}
