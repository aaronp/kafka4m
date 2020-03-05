package kafka4m.util

import com.typesafe.scalalogging.StrictLogging
import monix.execution.schedulers.SchedulerService
import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}

import scala.util.Try

object Schedulers {
  def using[A](f: Scheduler => A): A = using()(f)

  def using[A](sched: SchedulerService = compute())(f: Scheduler => A): A = {
    try {
      f(sched)
    } finally {
      sched.shutdown()
    }
  }

  object LoggingReporter extends UncaughtExceptionReporter with StrictLogging {
    override def reportFailure(ex: Throwable): Unit = {
      logger.error(s"Failure: $ex", ex)
    }
  }

  def io(name: String = "kafak4m-io", daemonic: Boolean = true, executionModel: ExecutionModel = ExecutionModel.Default): SchedulerService = {
    Scheduler.io(name, daemonic = daemonic, reporter = LoggingReporter, executionModel = executionModel)
  }

  def compute(name: String = "kafak4m-compute", daemonic: Boolean = true, executionModel: ExecutionModel = ExecutionModel.Default): SchedulerService = {
    Scheduler.computation(name = name, daemonic = daemonic, reporter = LoggingReporter, executionModel = executionModel)
  }

  def close(s: Scheduler): Unit = {
    Try {
      s match {
        case ss: SchedulerService => ss.shutdown()
        case c: AutoCloseable     => c.close()
        case _                    =>
      }
    }
  }
}
