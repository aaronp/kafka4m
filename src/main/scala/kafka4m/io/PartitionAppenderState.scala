package kafka4m.io

import kafka4m.partitions._
import monix.reactive.{Notification, Observable}

/**
  * A consumer of [[PartitionEvent]]s which feeds the events into an [[Appender]]
  */
private[io] object PartitionAppenderState {

  /**
    *
    * @param appendEvents the input events (append, flush)
    * @param newAppender a means to create a new appender from a key and the first value which applied to that key
    * @tparam A the data types we're being appended (e.g. byte arrays, byte buffers, other typed values)
    * @tparam K some sort of key used to route the values to a particular appender
    * @return an observable of keys with completed appenders
    */
  def partitionEvents[A, K, Writer <: Appender[A]](appendEvents: Observable[PartitionEvent[A, K]])(newAppender: (K, A) => Writer): Observable[(K, Writer)] = {
    appendEvents
      .scan(PartitionAppenderState[A, K, Writer](newAppender, Map.empty) -> Seq.empty[Notification[(K, Writer)]]) {
        case ((st8, _), next) => st8.update(next)
      }
      .flatMap {
        case (_, notifications) => Observable.fromIterable(notifications)
      }
      .dematerialize
  }
}

private[io] case class PartitionAppenderState[A, K, Writer <: Appender[A]](newAppender: (K, A) => Writer, byBucket: Map[K, Writer]) {
  private val NoOp = (this, Nil)
  def update(event: PartitionEvent[A, K]): (PartitionAppenderState[A, K, Writer], Seq[Notification[(K, Writer)]]) = {
    event match {
      case AppendData(bucket, data) =>
        byBucket.get(bucket) match {
          case Some(appender) =>
            appender.append(data)
            NoOp
          case None =>
            val appender = newAppender(bucket, data)
            appender.append(data)
            copy(byBucket = byBucket.updated(bucket, appender)) -> Nil
        }

      case ForceFlushBuckets(signalComplete) =>
        val onNexts = byBucket.map {
          case (bucket, appender) =>
            appender.close()
            Notification.OnNext(bucket -> appender)
        }
        val events = if (signalComplete) {
          onNexts.toSeq :+ Notification.OnComplete
        } else {
          onNexts.toSeq
        }
        copy(byBucket = Map.empty[K, Writer]) -> events

      case FlushBucket(bucket) =>
        byBucket.get(bucket) match {
          case Some(appender) =>
            appender.close()
            copy(byBucket = byBucket - bucket) -> Seq(Notification.OnNext(bucket -> appender))
          case None =>
            NoOp
        }
    }
  }
}
