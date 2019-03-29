package esa.rest
import args4c.ConfigApp
import com.typesafe.config.Config

object RestServerMain extends ConfigApp {
  type Result = RunningServer
  def run(config: Config): RunningServer = RunningServer(Settings(config))
}
