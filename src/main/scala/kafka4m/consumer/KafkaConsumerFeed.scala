package kafka4m.consumer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import args4c.implicits._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import monix.reactive.{Observable, Pipe}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object KafkaConsumerFeed {
  def apply(config: Config)(implicit sched: Scheduler): KafkaConsumerFeed[String, Array[Byte]] = {
    val consumerAccess: KafkaAccess[RichKafkaConsumer[String, Array[Byte]]] = KafkaAccess(RichKafkaConsumer.byteArrayValues(config))
    val topic                                                               = config.getString("kafka4m.consumer.topic")
    val feedTimeout                                                         = config.asFiniteDuration("kafka4m.consumer.feedTimeout")
    val commandQueueSize                                                    = config.getInt("kafka4m.consumer.commandQueueSize")
    val queue                                                               = new ArrayBlockingQueue[ConsumerRecord[String, Array[Byte]]](commandQueueSize)
    apply(topic, queue, feedTimeout, consumerAccess)
  }

  def apply[K, V](topic: String, consumerAccess: KafkaAccess[RichKafkaConsumer[K, V]])(implicit sched: Scheduler): KafkaConsumerFeed[K, V] = {
    apply(topic, new ArrayBlockingQueue[ConsumerRecord[K, V]](1000), 1.second, consumerAccess)
  }

  def apply[K, V](topic: String, data: BlockingQueue[ConsumerRecord[K, V]], feedTimeout: FiniteDuration, consumerAccess: KafkaAccess[RichKafkaConsumer[K, V]])(
      implicit sched: Scheduler) = {

    val feed = new KafkaConsumerFeed[K, V](topic, data, feedTimeout, consumerAccess)
    sched.execute(feed)
    feed
  }
}

case class KafkaConsumerFeed[K, V] private (topic: String,
                                            data: BlockingQueue[ConsumerRecord[K, V]],
                                            feedTimeout: FiniteDuration,
                                            consumerAccess: KafkaAccess[RichKafkaConsumer[K, V]])(implicit sched: Scheduler)
    extends Runnable
    with AutoCloseable
    with StrictLogging {
  private val cancelled = new AtomicBoolean(false)

  def unicast: Observable[ConsumerRecord[K, V]] = {
    val (input, output) = Pipe.publish[ConsumerRecord[K, V]].unicast
    sched.execute(() => {
      while (!cancelled.get()) {
        val next = data.take()
        Await.result(input.onNext(next), feedTimeout)
      }
    })
    output
  }

  def run(): Unit = {
    try {
      Await.result(consumerAccess(_.subscribe(topic, RebalanceListener)), feedTimeout * 10)
    } catch {
      case NonFatal(e) =>
        logger.error(s"ERROR DURING SUBSCRIPTION: $e")
        throw e
    }
    loop()
    logger.error("KafkaConsumerFeed complete!")
  }

  private def loop(): Unit = {
    while (!cancelled.get()) {
      try {
        logger.trace("polling kafka")
        val future: Future[Iterable[ConsumerRecord[K, V]]] = consumerAccess(_.poll())
        val d8a                                            = Await.result(future, feedTimeout)
        // blocking put ... wait if the queue is full
        d8a.foreach(data.put)
      } catch {
        case NonFatal(e) =>
          logger.error("kafka feed loop error", e)
      }
    }
  }

  override def close(): Unit = {
    consumerAccess.close()
  }
}
