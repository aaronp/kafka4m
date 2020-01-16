package kafka4m.consumer

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ListBuffer

class ConsumerPackageTest extends AnyWordSpec with Matchers with Eventually {
  "repeatedObservable" should {
    "not evaluate the provider until required" in {

      val calls       = new AtomicInteger(0)
      val invocations = new AtomicInteger(0)

      val obs: Observable[Int] = repeatedObservable {
        calls.incrementAndGet()
        Iterable(invocations.incrementAndGet(), invocations.incrementAndGet(), invocations.incrementAndGet())
      }

      val publisher = obs.toReactivePublisher
      object S extends Subscriber[Int] {
        var subscription: Subscription = null
        def request(n: Long) = {
          subscription.request(n)
        }
        override def onSubscribe(s: Subscription): Unit = {
          require(subscription == null)
          subscription = s
        }

        val received = ListBuffer[Int]()
        override def onNext(i: Int): Unit = {
          received += i
        }

        override def onError(t: Throwable): Unit = {
          fail(t.getMessage)
        }

        override def onComplete(): Unit = {
          fail("on complete not expected")
        }
      }

      publisher.subscribe(S)

      S.request(1)
      eventually {
        calls.get() shouldBe 1
        invocations.get() shouldBe 3
        S.received.size shouldBe 1
      }

      S.request(1)
      eventually {
        calls.get() shouldBe 1
        invocations.get() shouldBe 3
        S.received.size shouldBe 2
      }

      S.request(1)
      eventually {
        calls.get() shouldBe 2
        invocations.get() shouldBe 6
        S.received.size shouldBe 3
      }

      S.request(1)
      eventually {
        calls.get() shouldBe 2
        invocations.get() shouldBe 6
        S.received.size shouldBe 4
      }

      S.request(10)
      eventually {
        calls.get() shouldBe 5
        invocations.get() shouldBe 15
        S.received.size shouldBe 14
      }
    }
  }
}
