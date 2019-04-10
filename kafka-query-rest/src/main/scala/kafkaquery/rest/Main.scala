package kafkaquery.rest

import args4c.ConfigApp
import com.typesafe.config.Config
import kafkaquery.rest.ssl.SslConfig

/**
  * The main entry point for the REST service
  *
  * (If you change/rename this, be sure to update kafkaquery-deploy/src/main/resources/boot.sh and project/Build.scala)
  *
  */
object Main extends ConfigApp {
  type Result = RunningServer

  override protected val configKeyForRequiredEntries = "kafkaquery.requiredConfig"

  def run(uConfig: Config): RunningServer = {
    val config  = uConfig.resolve()
    val sslConf = SslConfig(config.getConfig("kafkaquery.tls"))
    RunningServer(Settings(config), sslConf)
  }
}
