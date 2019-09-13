package kafka4m.partitions

/**
  * The data passing through will be partitioned into different buckets. At some point we'll acknowledge
  * that there won't be anymore records coming through for a particular bucket (partition)
  */
sealed trait AppendEvent[A]
final case class AppendData[A](bucket: TimeBucket, record: A) extends AppendEvent[A]
final case class FlushBucket[A](bucket: TimeBucket)           extends AppendEvent[A]
final case class ForceFlushBuckets[A]()                       extends AppendEvent[A]
