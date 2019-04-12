package pipelines.rest.routes

import com.typesafe.scalalogging.StrictLogging
import org.reactivestreams.{Publisher, Subscriber, Subscription}

object LoggingPublisher {

  def apply[A](underlying: Publisher[A], prefix: String = "") = new Publisher[A] with StrictLogging {

    override def subscribe(foo: Subscriber[_ >: A]): Unit = {
      underlying.subscribe(new Subscriber[A] {
        override def onSubscribe(bar: Subscription): Unit = {
          val s = new Subscription {
            override def request(n: Long): Unit = {
              logger.debug(s"${prefix}subscription.request($n)")
              bar.request(n)
            }

            override def cancel(): Unit = {
              logger.debug(s"${prefix}subscription.cancel()")
              bar.cancel()
            }
          }
          logger.info(s"${prefix}onSubscribe($s)")
          foo.onSubscribe(s)
        }

        override def onNext(t: A): Unit = {
          logger.info(s"${prefix}onNext($t)")
          foo.onNext(t)
        }

        override def onError(t: Throwable): Unit = {
          logger.info(s"${prefix}onError($t)")
          foo.onError(t)
        }

        override def onComplete(): Unit = {
          logger.info(s"${prefix}onComplete()")
          foo.onComplete()
        }
      })
    }
  }
}
