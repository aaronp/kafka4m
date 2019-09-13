package kafka4m.partitions

import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

/**
  * we want to consume 'ConsumerRecords' from kafka and partition them into time buckets.
  *
  * Each bucket should be represented as an observable of some kind of data structure which contains the ConsumerRecords that completes
  * once we've consumed all* the records from the time bucket (e.g. if we want to partition the data into 10 minute segments, one partition
  * would contain the records from 8:10 to 8:20)
  *
  * We capture some logic in the state to ensure certainty of events.
  * The idea is that we'll be processing a number of consumer records which have a timestamp, and putting those records into some 'buckets' based on those times (e.g. "every 20 minutes").
  *
  * We could, however, receive one record at one timestamp, followed by another record at an earlier timestamp.
  *
  * To cope with that, this class introduces the concept of 'recordsReceivedBeforeFlush', which is meant to represent (after 'recordsReceivedBeforeFlush' events, it's safe to assume we won't see any more events for a particular bucket, and so can produce a 'flush' event for that bucket).
  *
  *
  */
final case class TimePartitionState[A: HasTimestamp] private (recordsReceivedBeforeClosingBucket: Int,
                                                              indicesRemaining: Long,
                                                              timeBucketSizeInMinutes: Int,
                                                              lastReceivedPerBucket: Map[TimeBucket, Long]) {

  private val hasTimestamp = HasTimestamp[A]
  def update(record: A, messageNumber: Long): (TimePartitionState[A], Seq[AppendEvent[A]]) = {
    val bucket      = TimeBucket(timeBucketSizeInMinutes, hasTimestamp.timestamp(record))
    val newBuckets  = lastReceivedPerBucket.updated(bucket, messageNumber)
    val appendEvent = AppendData(bucket, record)
    if (indicesRemaining == 0) {
      val threshold = messageNumber - recordsReceivedBeforeClosingBucket
      val (toFlush, remaining) = newBuckets.partition {
        case (_, lastReceivedIndex) => lastReceivedIndex <= threshold
      }
      val flushes  = toFlush.keysIterator.map(FlushBucket.apply[A])
      val newState = TimePartitionState[A](recordsReceivedBeforeClosingBucket, recordsReceivedBeforeClosingBucket, timeBucketSizeInMinutes, remaining)

      val events: Seq[AppendEvent[A]] = appendEvent +: flushes.toSeq
      (newState, events)
    } else {
      val newState: TimePartitionState[A] = copy(indicesRemaining = indicesRemaining - 1, lastReceivedPerBucket = newBuckets)
      (newState, Seq(appendEvent))
    }
  }
}

object TimePartitionState {

  /**
    *
    * @param source
    * @param recordsReceivedBeforeClosingBucket
    * @param timeBucketSize
    * @tparam A
    * @return a stream of append events from the source observable
    */
  def appendEvents[A: HasTimestamp](source: Observable[A], recordsReceivedBeforeClosingBucket: Int, timeBucketSize: FiniteDuration): Observable[AppendEvent[A]] = {
    val stateMachine: Observable[(TimePartitionState[A], Seq[AppendEvent[A]])] =
      source.zipWithIndex.scan(TimePartitionState(recordsReceivedBeforeClosingBucket, timeBucketSize) -> Seq.empty[AppendEvent[A]]) {
        case ((st8, _), (next, i)) => st8.update(next, i)
      }
    stateMachine.flatMap {
      case (_, events) => Observable.fromIterable(events)
    }
  }

  def apply[A: HasTimestamp](recordsReceivedBeforeClosingBucket: Int, timeBucketSize: FiniteDuration): TimePartitionState[A] = {
    new TimePartitionState[A](recordsReceivedBeforeClosingBucket, recordsReceivedBeforeClosingBucket, timeBucketSize.toMinutes.toInt, Map.empty)
  }
}
