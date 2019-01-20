package kafka4m.consumer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import com.typesafe.scalalogging.StrictLogging
import kafka4m.consumer.KafkaAccess.UseResource

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * Stupid Kafka clients enforce that they're single-threaded, so goodbye reactive streams.
  */
object KafkaAccess {

  final case class UseResource[I, O](thunk: I => O, promise: Promise[O], origin: String)

  def apply[R](newResource: => R)(implicit sched: ExecutionContext): KafkaAccess[R] = {
    apply(1000, () => newResource)
  }

  def apply[R](capacity: Int, newResource: () => R)(implicit sched: ExecutionContext): KafkaAccess[R] = {
    val queue  = new ArrayBlockingQueue[UseResource[R, _]](capacity)
    val access = new KafkaAccess(queue, newResource)
    sched.execute(access)
    access
  }
}

/**
  * KafkaConsumer is single-threaded, so we have to wrap access to it.
  *
  * @param newResource
  */
final case class KafkaAccess[R](queue: BlockingQueue[UseResource[R, _]], newResource: () => R) extends Runnable with AutoCloseable with StrictLogging {

  private val running = new AtomicBoolean(true)

  def apply[A](thunk: R => A)(implicit source: sourcecode.Enclosing): Future[A] = {
    val promise = Promise[A]()
    queue.add(new UseResource[R, A](thunk, promise, source.value))
    promise.future
  }

  override def run(): Unit = {
    logger.info("Running kafka connect in a single thread. Seriously.")
    try {
      loop(newResource())
      logger.info("Done")
    } catch {
      case NonFatal(fml) =>
        logger.error(s"Error creating KafkaConsumer: $fml")
    }

  }

  def loop(resource: R) = {
    while (running.get()) {
      queue.take() match {
        case null =>
        case UseResource(thunk, promise, from) =>
          try {
            val result = thunk(resource)
            promise.trySuccess(result)
          } catch {
            case NonFatal(e) =>
              promise.tryFailure(new Exception(s"Kafka call from $from failed with $e", e))
          }
      }
    }
    resource match {
      case ac: AutoCloseable => ac.close()
      case _                 =>
    }
  }

  override def close(): Unit = {
    running.compareAndSet(false, true)
  }
}
