package kafka4m.util

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

final case class Env(config: Config, compute: Scheduler, io: Scheduler) extends AutoCloseable {
  override def close(): Unit = {
    Env.close(compute)
    Env.close(io)
  }
}

object Env extends LazyLogging {
  def apply(config: Config): Env = {
    new Env(config, Schedulers.compute(), Schedulers.io())
  }

  def close(s: Scheduler) = s match {
    case ss: SchedulerService => ss.shutdown()
    case ac: AutoCloseable    => ac.close()
    case other =>
      logger.warn(s"NOT closing scheduler $other")
  }

}
