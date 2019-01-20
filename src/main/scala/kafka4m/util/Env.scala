package kafka4m.util

import com.typesafe.config.Config
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

final case class Env(config: Config, compute: Scheduler, bounded: Scheduler) extends AutoCloseable {
  override def close(): Unit = {
    Env.close(compute)
    Env.close(bounded)
  }
}

object Env {
  def apply(config: Config): Env = {
    new Env(config, Schedulers.compute(), Schedulers.io())
  }

  def close(s: Scheduler) = s match {
    case ss: SchedulerService => ss.shutdown()
    case ac: AutoCloseable => ac.close()
    case _ =>
  }

}
