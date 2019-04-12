package pipelines.connect

import java.util.concurrent.ScheduledExecutorService

import args4c.ConfigApp
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler

/**
  * The main entry point of the application
  */
object ConnectMain extends ConfigApp with StrictLogging {
  type Result = RunningApp

  override def defaultConfig(): Config = ConfigFactory.load()

  override def run(config: Config) = {
    logger.info("Running with:\n" + config.withPaths("pipelines").summary())

    implicit val scheduler = Scheduler.io("ConnectMain")
    val listener           = RichKafkaProducer.strings(config)
    RunningApp(listener, scheduler)
  }
}
