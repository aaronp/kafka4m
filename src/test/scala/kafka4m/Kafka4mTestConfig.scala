package kafka4m

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.config.{Config, ConfigFactory}

object Kafka4mTestConfig {

  private val id = new AtomicLong(System.currentTimeMillis)

  def newTopic() = s"testtopic-${id.incrementAndGet()}"

  def next(): (String, Config) = {
    val topic = newTopic()
    (topic, forTopic(topic))
  }
  def forConsumerGroup(group: String, autoOffsetReset: String = "earliest", fallback: Config = ConfigFactory.load()) = {
    val c = ConfigFactory.parseString(s"""kafka4m.consumer.group.id : "$group"
                                         |kafka4m.consumer.application.id : "$group-appid"
                                         |kafka4m.consumer.auto.offset.reset : $autoOffsetReset
            """.stripMargin)
    c.withFallback(fallback)
  }
  def forTopic(topic: String = newTopic(), fallback: Config = ConfigFactory.load()) = {
    val c = ConfigFactory.parseString(s"""
                                           |kafka4m.admin.topic=$topic
                                           |kafka4m.topic=$topic
                                           |kafka4m.consumer.topic=$topic
                                           |kafka4m.streams.topic=$topic
                                           |kafka4m.streams.application.id=$topic
                                           |kafka4m.producer.topic=$topic
                                           |
                                           |kafka4m.consumer.group.id : "test"$topic
                                           |kafka4m.consumer.application.id : "test"$topic
                                           |kafka4m.consumer.auto.offset.reset : earliest
            """.stripMargin)
    c.withFallback(fallback)

  }
}
