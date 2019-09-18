package kafka4m.partitions

/**
  * The data passing through will be partitioned into different buckets. At some point we'll acknowledge
  * that there won't be anymore records coming through for a particular bucket (partition)
  */
sealed trait PartitionEvent[A, K]
final case class AppendData[A, K](bucket: K, record: A) extends PartitionEvent[A, K]
final case class FlushBucket[A, K](bucket: K)           extends PartitionEvent[A, K]

/**
  * @param signalComplete
  * @tparam A
  * @tparam K
  */
final case class ForceFlushBuckets[A, K](signalComplete: Boolean) extends PartitionEvent[A, K]
