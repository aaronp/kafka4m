package kafka4m

import args4c.ConfigApp
import com.typesafe.config.Config

import scala.util.Try

object Kafka4mApp extends ConfigApp {
  override type Result = Unit

  override def run(config: Config) = {
    val action = Try(config.getString("kafka4m.action")).getOrElse("")
    action match {
      case "read"  =>
      case "write" =>
      case ""      =>
    }
  }
}
