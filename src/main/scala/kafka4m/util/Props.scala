package kafka4m.util

import java.util.{Properties, UUID}
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

object Props extends LazyLogging {

  type Bytes = Array[Byte]

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
  def replaceUniqueId(str: String, uid: String = UUID.randomUUID.toString): String = {
    str.replaceAllLiterally("{uniqueID}", uid)
  }

  private object AsInteger {

    def unapply(str: String): Option[Integer] = {
      Try(Integer.valueOf(str.trim)).toOption
    }
  }

  def newSchedulerService(): ScheduledExecutorService = {
    Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r)
        t.setName("flush-scheduler")
        t.setDaemon(true)
        t
      }
    })
  }

}
