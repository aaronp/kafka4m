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
final case class MiniBatchState[A, K] private (miniBatchSize: Int, indicesRemaining: Long, strict: Boolean, lastReceivedPerBucket: Map[K, Long])(
    implicit partitioner: Partitioner[A, K]) {

  def update(record: A, messageNumber: Long): (MiniBatchState[A, K], Seq[PartitionEvent[A, K]]) = {
    val bucket      = partitioner.bucketForValue(record)
    val appendEvent = AppendData(bucket, record)
    def newBuckets  = lastReceivedPerBucket.updated(bucket, messageNumber)
    if (indicesRemaining == 0) {
      if (strict) {
        val newState = new MiniBatchState[A, K](miniBatchSize, miniBatchSize, strict, Map.empty)
        val events = {
          val flushes = lastReceivedPerBucket.keysIterator.map(FlushBucket.apply[A, K])
          Iterator(appendEvent) ++ flushes
        }
        (newState, events.toSeq)
      } else {
        val threshold = messageNumber - miniBatchSize
        val (toFlush, remaining) = newBuckets.partition {
          case (_, lastReceivedIndex) => lastReceivedIndex <= threshold
        }
        val flushes  = toFlush.keysIterator.map(FlushBucket.apply[A, K])
        val newState = MiniBatchState[A, K](miniBatchSize, miniBatchSize, strict, remaining)

        val events: Seq[PartitionEvent[A, K]] = appendEvent +: flushes.toSeq
        (newState, events)
      }
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
  def byTime[A: HasTimestamp](source: Observable[A], miniBatchSize: Int, timeBucketSize: FiniteDuration): Observable[PartitionEvent[A, TimeBucket]] = {
    implicit val timePartitioner: Partitioner[A, TimeBucket] = Partitioner.byTime[A](timeBucketSize.toMinutes.toInt)
    partitionEvents(source, miniBatchSize, false)
  }

  /**
    * Return a stream of [[PartitionEvent]]s from the given observable
    *
    * @param source the original stream
    * @param miniBatchSize a 'number of events received' indicator used to determine how often a [[FlushBucket]] event is sent
    * @param strictBatchSizes if false, then the 'miniBatchSize' is used to indicate the number of 'A' values observed without the 'A' value appearing in a 'K' bucket before we flush
    * @param partitioner a means to partition values of A into buckets of type K
    * @tparam A
    * @tparam K the bucket (batch) type
    * @return an observable of events (append and flush) which
    */
  def partitionEvents[A, K](source: Observable[A], miniBatchSize: Int, strictBatchSizes: Boolean)(implicit partitioner: Partitioner[A, K]): Observable[PartitionEvent[A, K]] = {
    val stateMachine: Observable[(MiniBatchState[A, K], Seq[PartitionEvent[A, K]])] =
      source.zipWithIndex.scan(MiniBatchState[A, K](miniBatchSize, strictBatchSizes) -> Seq.empty[PartitionEvent[A, K]]) {
        case ((st8, _), (next, i)) => st8.update(next, i)
      }
    stateMachine.flatMap {
      case (_, events) => Observable.fromIterable(events)
    }
  }

  def apply[A, K](miniBatchSize: Int, strictBatchSizes: Boolean)(implicit partitioner: Partitioner[A, K]): MiniBatchState[A, K] = {
    new MiniBatchState[A, K](miniBatchSize, miniBatchSize, strictBatchSizes, Map.empty)
  }
}
