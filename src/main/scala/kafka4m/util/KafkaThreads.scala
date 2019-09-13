package kafka4m.util

import scala.collection.JavaConverters._
import scala.collection.mutable

object KafkaThreads {
  def apply(): mutable.Iterable[Thread] = {
    Thread.getAllStackTraces.asScala.collect {
      case (thread, stack) if thread.getName.startsWith("kafka") => thread
    }
  }

}
