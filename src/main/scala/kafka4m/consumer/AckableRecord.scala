package kafka4m.consumer

import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}

import cats.Functor
import monix.execution.schedulers.SchedulerService
import monix.execution.{ExecutionModel, Scheduler}
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition

import scala.concurrent.Future

/**
  * Represents a message with the components needed to commit the offsets/partitions to Kafka
  * @param consumer
  * @param offset
  * @param record
  * @tparam A
  */
final case class AckableRecord[A] private (consumer: RichKafkaConsumer[_, _], offset: PartitionOffsetState, record: A) {

  /**
    * Commits the current partition offset + 1 to Kafka so that, should we disconnect, we'll receive the next message
    * @return a future of the offsets
    */
  def commitPosition(): Future[Map[TopicPartition, OffsetAndMetadata]] = {
    consumer.commitAsync(offset.incOffsets())
  }

  /**
    * If we commit this offset, then on reconnect we would receive this same message again
    * @return the commit future
    */
  def commitConsumedPosition(): Future[Map[TopicPartition, OffsetAndMetadata]] = {
    consumer.commitAsync(offset)
  }

  /**
    * Maps the record type for this record
    * @param f
    * @tparam B
    * @return
    */
  final def map[B](f: A => B): AckableRecord[B] = copy(consumer, offset, f(record))
}

object AckableRecord {

  /** Creates a stream of ack-able records for this consumer
    *
    * @param closeOnComplete a flag to determine whether the kafka consumer should be closed when the stream completes
    * @param kafkaScheduler the single-threaded Kafka scheduler
    * @param makeConsumer a function for creating a consumer
    * @tparam K the key type
    * @tparam V the value type
    * @return a stream of [[AckableRecord]]s
    */
  def apply[K, V](closeOnComplete: Boolean, kafkaScheduler: Scheduler = singleThreadedScheduler())(
      makeConsumer: => RichKafkaConsumer[K, V]): Observable[AckableRecord[ConsumerRecord[K, V]]] = {
    val withConsumer: Observable[(RichKafkaConsumer[K, V], ConsumerRecord[K, V])] = singleThreadObservable(closeOnComplete, kafkaScheduler)(makeConsumer)

    withOffsets(withConsumer).map {
      case (state, (consumer, record)) =>
        new AckableRecord[ConsumerRecord[K, V]](consumer, state, record)
    }
  }

  /**
    * Create a kafka consumer observable which will ensure all the 'consumer.poll(...)' calls happen on a single thread
    * (lest we incur the wrath of apache Kafka "one and only one thread can use a KafkaConsumer" rule
    *
    * @param closeOnComplete
    * @param kafkaScheduler
    * @param makeConsumer
    * @tparam K
    * @tparam V
    * @return
    */
  def singleThreadObservable[K, V](closeOnComplete: Boolean, kafkaScheduler: Scheduler = singleThreadedScheduler())(
      makeConsumer: => RichKafkaConsumer[K, V]): Observable[(RichKafkaConsumer[K, V], ConsumerRecord[K, V])] = {
    Observable.delay(makeConsumer).executeOn(kafkaScheduler, true).flatMap { kafkaConsumer =>
      kafkaConsumer.asObservable(closeOnComplete)(kafkaScheduler).map { record =>
        (kafkaConsumer, record)
      }
    }
  }

  /**
    * combine the records with a means of tracking the offsets
    * @param records
    * @return
    */
  def withOffsets[A: HasRecord](records: Observable[A]): Observable[(PartitionOffsetState, A)] = {
    val initialTuple: (PartitionOffsetState, Option[A]) = (PartitionOffsetState(), Option.empty[A])
    records
      .scan(initialTuple) {
        case ((state, _), value) =>
          val kafkaMsg = HasRecord[A].recordFor(value)
          (state.update(kafkaMsg), Option(value))
      }
      .collect {
        case (state, Some(a)) => (state, a)
      }
  }

  def singleThreadedScheduler(name: String = "SingleThreadForKafkaRead"): SchedulerService = {
    Scheduler(singleThreadedExecutor(name), ExecutionModel.SynchronousExecution)
  }

  def singleThreadedExecutor(name: String): ExecutorService = {
    singleThreadedExecutor { thread =>
      thread.setName(name)
      thread.setDaemon(true)
      thread
    }
  }

  def singleThreadedExecutor(prepare: Thread => Thread): ExecutorService = {
    Executors.newSingleThreadExecutor(new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        prepare(new Thread(r))
      }
    })
  }

  implicit object AckableRecordFunctor extends Functor[AckableRecord] {
    override def map[A, B](fa: AckableRecord[A])(f: A => B): AckableRecord[B] = fa.map(f)
  }

}
