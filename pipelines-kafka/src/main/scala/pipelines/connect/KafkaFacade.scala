package pipelines.connect

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import pipelines.kafka.{ListTopicsResponse, PullLatestResponse}

import scala.concurrent.duration.FiniteDuration
import scala.io.Source

/**
  * Represents the pieces of Kafka which we access from our REST endpoints
  */
trait KafkaFacade extends AutoCloseable {
  def listTopics(): ListTopicsResponse
  def pullLatest(topic: String, offset: Long, limit: Long): PullLatestResponse

  def schemaForTopic(topic: String): Option[String]
}

object KafkaFacade extends LazyLogging {

  def apply(consumer: RichKafkaConsumer[String, Bytes],
            schemasByTopic: Map[String, String],
            pollTimeout: FiniteDuration,
            timeout: FiniteDuration,
            closeConsumer: RichKafkaConsumer[String, Bytes] => Unit): KafkaFacade = {
    new KafkaFacade with AutoCloseable with StrictLogging {
      override def listTopics() = ListTopicsResponse(consumer.listTopics())
      override def pullLatest(topic: String, offset: Long, limit: Long) = {
        consumer.pullLatest(topic, limit, pollTimeout, timeout, identity)
      }

      override def schemaForTopic(topic: String): Option[String] = {
        schemasByTopic.get(topic)
      }

      override def close(): Unit = {
        closeConsumer(consumer)
      }
    }
  }

  def schemasByTopicForRootConfig(rootConfig: Config) = schemasByTopicForConfig(rootConfig.getConfig("pipelines.avro"))

  def schemasByTopicForConfig(avroConfig: Config): Map[String, String] = {
    import args4c.implicits._

    import scala.collection.JavaConverters._
    val confList = avroConfig.getConfigList("schemas").asScala.map(_.config)
    val pears = confList.map { entry =>
      val topic  = entry.getString("topic")
      val path   = entry.getString("schema")
      val schema = loadSchema(path)
      logger.info(s"Associating schema '$path' with topic $topic")
      (topic, schema)
    }
    pears.groupBy(_._1).map {
      case (key, values) =>
        val only = values.map(_._2).distinct.ensuring(_.size == 1, s"multiple configuration entries found for topic '${key}': ${values.mkString(",")}}").head
        (key, only)
    }
  }

  def loadSchema(schemaTextOrPath: String) = {
    schemaFromFile(schemaTextOrPath).orElse(schemaOnClasspath(schemaTextOrPath)).getOrElse(schemaTextOrPath)
  }

  def schemaFromFile(path: String): Option[String] = {
    import eie.io._
    logger.info(s"Loading schema from file: $path")
    Option(path.asPath).filter(_.isFile).map(_.text)
  }

  def schemaOnClasspath(path: String) = {
    val urlOpt: Option[URL] = Option(getClass.getClassLoader.getResource(path))
    urlOpt.map { url =>
      logger.info(s"Loading schema from classpath: $path")
      Source.fromURI(url.toURI).getLines().mkString("\n")
    }
  }
}
