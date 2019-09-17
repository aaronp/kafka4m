package kafka4m.partitions

import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration

/**
  * State which represents the consumption of 'ConsumerRecords' from kafka and partition them into buckets.
  *
  * Each bucket should be represented as an observable of some kind of data structure which contains the ConsumerRecords that completes
  * once we've consumed all* the records from the time bucket (e.g. if we want to partition the data into 10 minute segments, one partition
  * would contain the records from 8:10 to 8:20)
  *
  * @param miniBatchSize
  * @param indicesRemaining
  * @param lastReceivedPerBucket
  * @param partitioner
  * @tparam A
  * @tparam K
  */
final case class MiniBatchState[A, K] private (miniBatchSize: Int, indicesRemaining: Long, lastReceivedPerBucket: Map[K, Long])(implicit partitioner: Partitioner[A, K]) {

  def update(record: A, messageNumber: Long): (MiniBatchState[A, K], Seq[BatchEvent[A, K]]) = {
    val bucket      = partitioner.bucketForValue(record)
    val newBuckets  = lastReceivedPerBucket.updated(bucket, messageNumber)
    val appendEvent = AppendData(bucket, record)
    if (indicesRemaining == 0) {
      val threshold = messageNumber - miniBatchSize
      val (toFlush, remaining) = newBuckets.partition {
        case (_, lastReceivedIndex) => lastReceivedIndex <= threshold
      }
      val flushes  = toFlush.keysIterator.map(FlushBucket.apply[A, K])
      val newState = MiniBatchState[A, K](miniBatchSize, miniBatchSize, remaining)

      val events: Seq[BatchEvent[A, K]] = appendEvent +: flushes.toSeq
      (newState, events)
    } else {
      val newState: MiniBatchState[A, K] = copy(indicesRemaining = indicesRemaining - 1, lastReceivedPerBucket = newBuckets)
      (newState, Seq(appendEvent))
    }
  }
}

object MiniBatchState {

  /** An example of batching by time (in minutes)
    *
    * @param source the data source
    * @param miniBatchSize the number of records to observe beyond the time before 'flushing' the bucket
    * @param timeBucketSize the bucket size (reduced to minutes) for the batches
    * @tparam A
    * @return a stream of append events from the source observable
    */
  def byTime[A: HasTimestamp](source: Observable[A], miniBatchSize: Int, timeBucketSize: FiniteDuration): Observable[BatchEvent[A, TimeBucket]] = {
    implicit val timePartitioner: Partitioner[A, TimeBucket] = Partitioner.byTime[A](timeBucketSize.toMinutes.toInt)
    partitionEvents(source, miniBatchSize)
  }

  def partitionEvents[A, K](source: Observable[A], miniBatchSize: Int)(implicit partitioner: Partitioner[A, K]): Observable[BatchEvent[A, K]] = {
    val stateMachine: Observable[(MiniBatchState[A, K], Seq[BatchEvent[A, K]])] =
      source.zipWithIndex.scan(MiniBatchState[A, K](miniBatchSize) -> Seq.empty[BatchEvent[A, K]]) {
        case ((st8, _), (next, i)) => st8.update(next, i)
      }
    stateMachine.flatMap {
      case (_, events) => Observable.fromIterable(events)
    }
  }

  def apply[A, K](miniBatchSize: Int)(implicit partitioner: Partitioner[A, K]): MiniBatchState[A, K] = {
    new MiniBatchState[A, K](miniBatchSize, miniBatchSize, Map.empty)
  }
}
