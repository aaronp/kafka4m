package kafka4m.jmx

import java.util.concurrent.atomic.AtomicLong

/**
  * A basic representation of some throughput
  * @param latestMessageCount the number of messages either sent or received in the last 'countSince' milliseconds
  * @param elapsedMillisSinceLastFlush the number of milliseconds which have elapsed since the last Throughput record was produced
  * @param totalMessages the total message count
  */
final case class Throughput(latestMessageCount: Long, elapsedMillisSinceLastFlush: Long, totalMessages: Long) {
  override def toString: String = s"${meanMessagesPerSecond}/second, $totalMessages total, $latestMessageCount in the past ${elapsedMillisSinceLastFlush}ms"
  def meanMessagesPerSecond: Int = {
    val perSecondScale = elapsedMillisSinceLastFlush.toDouble / 1000
    if (perSecondScale <= 0.0) {
      0
    } else {
      val meanPerSecond = latestMessageCount.toDouble / perSecondScale
      meanPerSecond.toInt
    }
  }
}

object Throughput {
  class Builder {
    val total                            = new AtomicLong(0)
    @volatile private var lastFlushEpoch = 0L
    @volatile private var lastTotal      = 0L

    def inc() = {
      total.incrementAndGet()
    }

    def currentThroughput(now: Long = System.currentTimeMillis()): Throughput = {
      val currentTotal = total.get()
      Throughput(
        latestMessageCount = currentTotal - lastTotal,
        elapsedMillisSinceLastFlush = now - lastFlushEpoch,
        totalMessages = currentTotal
      )
    }
    def flush(now: Long = System.currentTimeMillis()): Throughput = {
      val throughput = currentThroughput(now)
      // only roll the throughput/second if we've observed >= 95% of a second.
      // this is a cheeky hack to allow different thread to 'flush' or to flush more frequently than once / second
      if (throughput.elapsedMillisSinceLastFlush >= 950) {
        lastFlushEpoch = now
        lastTotal = throughput.totalMessages
      }
      throughput
    }
  }
}
