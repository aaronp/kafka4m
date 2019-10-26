package kafka4m.util

import com.typesafe.scalalogging.StrictLogging
import monix.execution.schedulers.SchedulerService
import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}

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

  def io(name: String = "kafak4m-io", daemonic: Boolean = true): SchedulerService = {
    Scheduler.io(name, daemonic = daemonic, reporter = LoggingReporter, executionModel = ExecutionModel.Default)
  }

  def compute(name: String = "kafak4m-compute", daemonic: Boolean = true, executionModel: ExecutionModel = ExecutionModel.Default): SchedulerService = {
    Scheduler.computation(name = name, daemonic = daemonic, reporter = LoggingReporter, executionModel = executionModel)
  }
}
