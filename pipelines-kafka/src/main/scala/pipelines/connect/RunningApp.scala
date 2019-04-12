package pipelines.connect

import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

import scala.concurrent.Future
import scala.util.control.NonFatal

case class RunningApp(kafkaPublisher: AutoCloseable, scheduler: Scheduler) extends AutoCloseable with StrictLogging {
  private def safeClose(name: String, c: AutoCloseable) = {
    import scala.concurrent.ExecutionContext.Implicits._
    try {
      Future {
        logger.info(s"Closing $name")
        c.close()
        logger.info(s"Closed $name")
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Error closing $name: $e", e)
    }
  }

  override def close(): Unit = {
    safeClose("kafkaPublisher", kafkaPublisher)
    safeClose("scheduler", new AutoCloseable {
      override def close(): Unit = {
        scheduler match {
          case ss: SchedulerService => ss.shutdown()
          case _                    =>
        }
      }
    })
  }
}
