package kafka4m.partitions

/**
  * A means to place values of type A into buckets of type B
  * @tparam A the input data type
  * @tparam B the 'bucket' partition
  */
trait Partitioner[A, B] {

  /** @param value a data type
    * @return a bucket for the given value
    */
  def bucketForValue(value: A): B

}

object Partitioner {
  def byTime[A: HasTimestamp](minutesPerBucket: Int) = new Partitioner[A, TimeBucket] {
    override def bucketForValue(value: A): TimeBucket = {
      val time = HasTimestamp[A].timestamp(value)
      TimeBucket(minutesPerBucket, time)
    }

  }
}
