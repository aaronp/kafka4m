package kafka4m.util

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}

/**
  * Kafka consumers need to be single-threaded, so this ensures we can run our kafka observable on a single thread
  */
case class FixedScheduler(name: String = FixedScheduler.nextKafkaSchedulerName(),
                          daemonic: Boolean = false,
                          executionModel: ExecutionModel = ExecutionModel.Default,
                          exceptionReporter: UncaughtExceptionReporter = Schedulers.LoggingReporter) {

  def execService(): ExecutorService = {
    Executors.newSingleThreadExecutor(new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        val thread = new Thread(r)
        thread.setName(name)
        thread.setDaemon(daemonic)
        thread
      }
    })
  }
  lazy val scheduler = Scheduler(execService(), exceptionReporter, executionModel)
}
object FixedScheduler {

  private val instanceCounter = new AtomicInteger(0)
  private def nextKafkaSchedulerName() = {
    val name = sys.env.getOrElse("KAFKA4M_FIXED_NAME", "kafka4m-consumer")
    instanceCounter.getAndIncrement() match {
      case 0 => name
      case n => s"$name-$n"
    }
  }
}
