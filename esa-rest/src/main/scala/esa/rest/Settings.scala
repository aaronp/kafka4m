package esa.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import esa.rest.ssl.SslConfig

case class Settings(rootConfig: Config, host: String, port: Int, sslConfig: SslConfig, materializer: ActorMaterializer) {

  object implicits {
    implicit val actorMaterializer: ActorMaterializer = materializer
    implicit val system                               = actorMaterializer.system
    implicit val executionContext                     = system.dispatcher
    implicit val routingSettings: RoutingSettings     = RoutingSettings(system)
  }
}

object Settings {

  def apply(rootConfig: Config): Settings = {
    val config                                   = rootConfig.getConfig("esa")
    implicit val system                          = ActorSystem(Main.getClass.getSimpleName.filter(_.isLetter))
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    new Settings(rootConfig, host = config.getString("host"), port = config.getInt("port"), sslConfig = SslConfig(config.getConfig("tls")), materializer)
  }
}
