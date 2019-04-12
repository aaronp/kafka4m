package pipelines.rest

import java.nio.file.Path

import args4c.ConfigApp
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import pipelines.rest.ssl.{GenCerts, SslConfig}

/**
  * The main entry point for the REST service
  *
  * (If you change/rename this, be sure to update pipelines-deploy/src/main/resources/boot.sh and project/Build.scala)
  *
  */
object Main extends ConfigApp with StrictLogging {
  type Result = RunningServer

  override protected val configKeyForRequiredEntries = "pipelines.requiredConfig"

  def run(config: Config): RunningServer = {

    import eie.io._
    val certPath = config.getString("pipelines.tls.certificate")
    val preparedConf = if (!certPath.asPath.isFile && config.hasPath("generateMissingCerts")) {
      ensureCert(certPath)
      config.set("pipelines.tls.password", "password")
    } else {
      config
    }

    val sslConf = SslConfig(preparedConf.getConfig("pipelines.tls"))
    RunningServer(Settings(preparedConf), sslConf)
  }

  /**
    * Create our own self-signed cert (if required) for local development
    */
  def ensureCert(pathToCert: String): Path = {
    import eie.io._
    val certFile = pathToCert.asPath
    if (!certFile.isFile) {
      logger.info(s"${certFile} doesn't exist, creating it")
      val pwd                          = "password"
      val (resValue, buffer, certPath) = GenCerts.genCert(certFile.getParent, certFile.fileName, "localhost", pwd, pwd, pwd)
      logger.info(s"created ${certPath}:\n\n${buffer.allOutput}\n\n")
      require(resValue == 0, s"Gen cert script exited w/ non-zero value $resValue")
    } else {
      logger.info("dev cert exists, cracking on...")
    }
    certFile
  }
}
