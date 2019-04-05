package kafkaquery.rest
import java.nio.file.Path

import args4c.{ConfigApp, SecretConfig}
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


        val pathToSecretConfig = "/app/config/"

  override protected def runWithConfig(secretConfig: SecretConfigResult, parsedConfig: Config): Option[Result] = {
    secretConfig match {
      case SecretConfigParsed(path: Path, config: Config) => super.runWithConfig(secretConfig, parsedConfig)
      case SecretConfigDoesntExist(path: Path) =>
      case SecretConfigNotSpecified =>

    }
  }

  def run(config: Config): RunningServer = {
    if (Settings.requiresSetup(config)) {
      RunningServer.setup(Settings(config))
    } else {
      val sslConf: SslConfig = SslConfig(config.getConfig("kafkaquery.tls"))
      RunningServer(Settings(config), sslConf)
    }
  }
}
