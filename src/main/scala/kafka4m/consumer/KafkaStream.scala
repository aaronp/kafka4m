package kafka4m.consumer
import args4c.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import kafka4m.AckBytes
import kafka4m.consumer.ConcurrentStream.KafkaFacade
import kafka4m.util.FixedScheduler
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * Combines our concurrent stream and kafka access
  *
  * @param stream
  * @param access
  * @tparam A
  */
class KafkaStream[A] private (val stream: ConcurrentStream[A], val access: ConsumerAccess) extends ConsumerAccess {
  override type Key   = access.Key
  override type Value = access.Value

  override def withConsumer[A](thunk: RichKafkaConsumer[access.Key, access.Value] => A): Future[A] = {
    access.withConsumer(thunk)
  }
}
object KafkaStream {

  def apply(config: Config = ConfigFactory.load(), kafkaScheduler: Scheduler = FixedScheduler().scheduler)(implicit ioScheduler: Scheduler): Task[KafkaStream[AckBytes]] = {

    val task = Task {
      val awaitJobTimeout: FiniteDuration = config.asFiniteDuration("kafka4m.jobs.awaitJobTimeout")
      val retryDuration: FiniteDuration   = config.asFiniteDuration("kafka4m.jobs.retryDuration")
      val minCommitFrequency              = config.getInt("kafka4m.jobs.minCommitFrequency")

      val closeOnComplete                                       = kafka4m.closeConsumerOnComplete(config)
      val kafkaConsumer: RichKafkaConsumer[String, Array[Byte]] = RichKafkaConsumer.byteArrayValues(config, kafkaScheduler, ioScheduler)

      val records: Observable[ConsumerRecord[String, Array[Byte]]] = kafkaConsumer.asObservable(closeOnComplete)
      val recordsAndOffsets                                        = AckableRecord.withOffsets(records)
      val acks: Observable[AckBytes] = recordsAndOffsets.map {
        case (offset, record) =>
          new AckableRecord(kafkaConsumer, offset, record)
      }

      val access: ConsumerAccess = kafkaConsumer
      val stream = ConcurrentStream(
        acks,
        asyncScheduler = ioScheduler,             //
        kafkaFacade = KafkaFacade(kafkaConsumer), //
        minCommitFrequency = minCommitFrequency,  //
        awaitJobTimeout = awaitJobTimeout,        //
        retryDuration = retryDuration
      )
      new KafkaStream(stream, access)
    }

    // we need to connect to kafka with a task run on an explicit scheduler to ensure kafka jobs always happen on that thread
    task.executeOn(kafkaScheduler)
  }
}
