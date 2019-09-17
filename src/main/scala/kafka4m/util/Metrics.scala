package kafka4m.util

import monix.eval.Task

/** Represents a simple counter task */
final class Metrics() {
  @volatile private var counter = 0
  @volatile private var total   = 0L

  val incThroughput: Task[Unit] = Task {
    counter = counter + 1
    total = total + 1
  }
  def flush(): LatencySnapshot = {
    val snap = LatencySnapshot(counter, total)
    counter = 0
    snap
  }
}
