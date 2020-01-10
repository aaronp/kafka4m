package kafka4m.producer

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import monix.execution.{Ack, Callback, Cancelable, Scheduler}
import monix.reactive.Observer

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

private[producer] class KafkaProducerObserver[A, K, V](asRecord: AsProducerRecord.Aux[A, K, V],
                                                       rkp: RichKafkaProducer[K, V],
                                                       scheduler: Scheduler,
                                                       cancelable: Cancelable,
                                                       unsafeCallback: Callback[Throwable, Long],
                                                       fireAndForget: Boolean,
                                                       continueOnError: Boolean)
    extends Observer[A]
    with LazyLogging {
  private val callback                                          = Callback.safe(unsafeCallback)(scheduler)
  private val sentCount                                         = new AtomicLong(0)
  @volatile private var result: Option[Either[Throwable, Long]] = None

  private lazy val tidyUp = {
    Try(rkp.close()).isSuccess
  }

  override def onNext(elem: A): Future[Ack] = {
    try {
      onNextUnsafe(elem)
    } catch {
      case NonFatal(e) =>
        logger.error(s"onNext error on $elem: ${e.getMessage}", e)
        if (continueOnError) {
          Ack.Continue
        } else {
          callback.onError(e)
          Ack.Stop
        }
    }
  }

  private def onNextUnsafe(elem: A): Future[Ack] = {
    val sent = sentCount.incrementAndGet()

    val record = asRecord.asRecord(elem)
    logger.trace(s"onNext($elem) sending record #$sent -> $record")
    if (fireAndForget) {
      rkp.sendRecord(record, LoggingCallback)
      Ack.Continue
    } else {
      val promise = PromiseCallback()
      rkp.sendRecord(record, promise)
      promise.future.map { _ =>
        Ack.Continue
      }(scheduler)
    }
  }

  override def onError(ex: Throwable): Unit = {
    callback.onError(ex)
    cancelable.cancel()
    tidyUp
  }

  override def onComplete(): Unit = {
    val numSent = sentCount.get()
    callback.onSuccess(numSent)
    tidyUp
  }
}
