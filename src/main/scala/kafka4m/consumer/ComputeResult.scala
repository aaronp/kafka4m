package kafka4m.consumer

import kafka4m.consumer.ConcurrentStream.ZipOffset

import scala.concurrent.Future

/**
  * A tuple of all the information at hand from a [[ConcurrentStream]] computation
  *
  * @param localConsumedOffset
  * @param kafkaRecord
  * @param taskResult
  * @tparam A
  * @tparam B
  */
case class ComputeResult[A, B](localConsumedOffset: ZipOffset, kafkaRecord: AckableRecord[A], taskResult: B) extends ConsumerAccess {
  def offset = kafkaRecord.offset
  override type Key   = kafkaRecord.Key
  override type Value = kafkaRecord.Value
  def input: A                  = kafkaRecord.record
  override def toString: String = s"ComputeResult #$localConsumedOffset (${kafkaRecord.record} -> $taskResult)"

  override def withConsumer[A](thunk: RichKafkaConsumer[kafkaRecord.access.Key, kafkaRecord.access.Value] => A): Future[A] = {
    kafkaRecord.withConsumer(thunk)
  }
}
