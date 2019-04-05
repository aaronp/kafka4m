package kafkaquery.connect

import java.util.concurrent.ScheduledExecutorService

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Future
import scala.util.control.NonFatal

case class RunningApp(kafkaPublisher: AutoCloseable, scheduler: ScheduledExecutorService) extends AutoCloseable with StrictLogging {
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
      override def close(): Unit = scheduler.shutdown()
    })
  }
}
