package esa.rest
import args4c.ConfigApp
import com.typesafe.config.Config

/**
  * The main entry point for the REST service
  *
  * (If you change/rename this, be sure to update esa-deploy/src/main/resources/boot.sh and project/Build.scala)
  *
  */
object Main extends ConfigApp {
  type Result = RunningServer
  def run(config: Config): RunningServer = RunningServer(Settings(config))
}
