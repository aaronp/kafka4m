package kafkaquery.eval

import java.util.concurrent.{CountDownLatch, TimeUnit}

import kafkaquery.kafka.StreamStrategy
import monix.eval.Task
import monix.execution.Cancelable
import monix.reactive.{Observable, Observer}
import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.{Matchers, WordSpec}

import concurrent.duration._

class KafkaReactiveTest extends WordSpec with Matchers {

  "KafkaReactive.apply" should {
    "work" ignore {

      val ints = Observable.fromIterator(Task.eval(Iterator.from(0))).delayOnNext(1.millis)

      val reactive: Observable[Int] = KafkaReactive(ints, messageLimitPerSecond = Option(100), StreamStrategy.Latest)

      val done = new CountDownLatch(1)
      object S extends Subscriber[Int] {
        var subscription: Subscription = null
        override def onSubscribe(s: Subscription): Unit = {
          println(s"onSubscribe($s)")
          require(subscription == null)
          subscription = s
          subscription.request(1)
        }

        override def onNext(t: Int): Unit = {
          println(s"onNext($t)")
          Thread.sleep(2000)
          subscription.request(1)
          if (t > 10000) {
            done.countDown()
          }
        }

        override def onError(t: Throwable): Unit = {
          println(s"oops: $t")
          subscription.cancel()
        }

        override def onComplete(): Unit = {
          println("done ... ?")
        }
      }
      implicit val scheduler = monix.execution.Scheduler.global
      val cancel = Cancelable { () =>
        println("Cancel, dammit!")
      }
      val s = Observer.fromReactiveSubscriber(S, cancel)
      reactive.subscribe(s)

      println("waiting...")
      done.await(10.seconds.toMillis, TimeUnit.MILLISECONDS)
      println("done")
    }
  }
  "KafkaReactive.select" should {
    val input = (1 to 17).toList
    List(
      0 -> Nil,
      1 -> List(1),
      2 -> List(1, 8)
    )
    "choose an even sample" in {
      KafkaReactive.select(input, 0) should be(empty)
      KafkaReactive.select(input, 1) shouldBe Seq(1)
      KafkaReactive.select(input, 2) shouldBe Seq(1, 9)
      KafkaReactive.select(input, 3) shouldBe Seq(1, 6, 11)
      KafkaReactive.select(input, 12) shouldBe (1 to 12)
      KafkaReactive.select(input, input.size) shouldBe input
      KafkaReactive.select(input, input.size + 1) shouldBe input
      KafkaReactive.select(input, input.size + 2) shouldBe input
      KafkaReactive.select(input, input.size + 3) shouldBe input
      KafkaReactive.select(input, input.size * 2) shouldBe input
      KafkaReactive.select(input, Int.MaxValue) shouldBe input
    }
  }
}
