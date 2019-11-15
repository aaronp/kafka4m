package kafka4m.util

import java.util.{Properties, UUID}

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

/**
  * @define TOPIC Due to config resolution rules, other applications can't simply set "kafka4m.topic" and have that apply to the "kafka4m.XXX.topic" setting (e.g. "kafka4m.consumer.topic", "kafka4m.producer.topic", etc)
  */
object Props extends LazyLogging {

  type Bytes = Array[Byte]

  /** $TOPIC
    *
    * @param rootConfig
    * @param subconfName
    * @return the non-empty topic from kafka4m.<subconfName>.topic
    */
  def topic(rootConfig: Config, subconfName: String, orElse: String*): String = {
    val foundOpt = (subconfName +: orElse).collectFirst {
      case subconf if rootConfig.getString(s"kafka4m.${subconf}.topic").nonEmpty => rootConfig.getString(s"kafka4m.${subconf}.topic")
    }

    foundOpt.getOrElse(rootConfig.getString("kafka4m.topic"))
  }

  /**
    * $TOPIC
    *
    * @param rootConfig
    * @param subconfName
    * @param orElse
    */
  def topics(rootConfig: Config, subconfName: String, orElse: String*): Set[String] = {
    topic(rootConfig, subconfName, orElse: _*).split(",", -1).map(_.trim).toSet
  }

  def replaceUniqueId(str: String, uid: String = UUID.randomUUID.toString): String = {
    str.replaceAllLiterally("{uniqueID}", uid)
  }

  def format(all: Properties): String = {
    import scala.collection.JavaConverters._
    all
      .keySet()
      .asScala
      .map(_.toString)
      .map { key =>
        val value = all.getProperty(key)
        s"${key} : ${value}"
      }
      .mkString(";\n")
  }

  def propertiesForConfig(config: Config): Properties = {
    import args4c.implicits._
    config
      .collectAsStrings()
      .foldLeft(new java.util.Properties) {
        case (props, (key, AsInteger(value))) =>
          props.put(key, value)
          props
        case (props, (key, value)) =>
          props.put(key, value)
          props
      }
  }

  private object AsInteger {
    def unapply(str: String): Option[Integer] = {
      Try(Integer.valueOf(str.trim)).toOption
    }
  }
}
