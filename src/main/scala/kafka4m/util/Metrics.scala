package kafka4m.util

import kafka4m.Kafka4mApp.reportThroughput
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}

import scala.concurrent.duration._

class Metrics() {
  @volatile private var counter = 0
  @volatile private var total   = 0

  val incThroughput: Task[Unit] = Task {
    counter = counter + 1
    total = total + 1
  }

  def start(scheduler: Scheduler): Cancelable = {
    scheduler.scheduleAtFixedRate(1.second, 1.second) {
      counter = 0
      reportThroughput(counter, total)
    }
  }
}
