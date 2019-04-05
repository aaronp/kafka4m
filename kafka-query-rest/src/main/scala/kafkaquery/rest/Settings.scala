package kafkaquery.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.RoutingSettings
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import kafkaquery.rest.ssl.SslConfig

case class Settings(rootConfig: Config, host: String, port: Int, jwtSecret: String, materializer: ActorMaterializer) {

  object implicits {
    implicit val actorMaterializer: ActorMaterializer = materializer
    implicit val system                               = actorMaterializer.system
    implicit val executionContext                     = system.dispatcher
    implicit val routingSettings: RoutingSettings     = RoutingSettings(system)
  }
}

object Settings {

  /** Do we need to configure this application?
    *
    * The answer is YES if:
    *
    * - the configuration doesn't specify a tls seed
    * - the configuration doesn't specify a jwt seed
    * - the configuration doesn't specify a valid certificate path
    * - there isn't at least one user or an admin role
    *
    * @param rootConfig the application configuration
    * @return true if we need to set up/configure this service for first time usage (e.g. set up certificates, users, etc)
    */
  def requiresSetup(rootConfig: Config): Boolean = {
    val tlsConf = rootConfig.getConfig("kafkaquery.tls")
    SslConfig.certPathOpt(tlsConf).isEmpty ||
    SslConfig.tlsSeedOpt(tlsConf).isEmpty ||
    jwtSecret(rootConfig).isEmpty
  }

  def jwtSecret(config: Config) = config.getString("kafkaquery.jwt.secret").trim

  def apply(rootConfig: Config): Settings = {
    val config                                   = rootConfig.getConfig("kafkaquery")
    implicit val system                          = ActorSystem(Main.getClass.getSimpleName.filter(_.isLetter))
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    new Settings(rootConfig, //
                 host = config.getString("host"), //
                 port = config.getInt("port"), //
                 jwtSecret = jwtSecret(rootConfig), //
                 materializer //
    )
  }
}
