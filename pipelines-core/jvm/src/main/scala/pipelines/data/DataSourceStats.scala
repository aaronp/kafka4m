package pipelines.data

import java.time
import java.time.ZonedDateTime

import monix.reactive.Observable
import pipelines.core.{AnyType, DataType}

import scala.concurrent.duration._
import scala.reflect.ClassTag

class DataSourceStats[A](source: Observable[A]) extends DataSource[StreamStatistics[A]] {
  override def tag: ClassTag[StreamStatistics[A]] = {
    val c1ass = classOf[StreamStatistics[A]]
    ClassTag[StreamStatistics[A]](c1ass)
  }

  override def sourceType: DataType = AnyType(tag.runtimeClass.getName)

  override def data: Observable[StreamStatistics[A]] = {
    source.bufferTimed(1.second).scan(StreamStatistics[A](None, 0, ZonedDateTime.now, 0, 0, ZonedDateTime.now)) {
      case (stats, seq) =>
        val now                 = ZonedDateTime.now
        val diff: time.Duration = java.time.Duration.between(stats.connectedAt, now)
        val total               = stats.totalReceived + seq.size
        val avePerSecond = diff.getSeconds match {
          case 0       => 0
          case seconds => total / seconds
        }
        StreamStatistics[A](seq.lastOption, total, stats.connectedAt, seq.size, avePerSecond.toInt, now)
    }
  }
}
