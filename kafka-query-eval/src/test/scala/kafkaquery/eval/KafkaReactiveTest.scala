package kafkaquery.eval

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, TimeUnit}

import kafkaquery.kafka.{QueryRequest, Rate, StreamStrategy, UpdateFeedRequest}
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, Observer}
import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.concurrent.Eventually
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class KafkaReactiveTest extends WordSpec with Matchers with Eventually with GivenWhenThen {

  def testTimeout: FiniteDuration = 3.seconds

  "KafkaReactive.source" should {
    "close and reconnect a new source when a new query request is made" in {
      Given("A queryable data source")
      class TestData(data: Iterable[Int]) extends Iterable[Int] with AutoCloseable {
        val closeCalls = new AtomicInteger(0)
        override def close(): Unit = {
          closeCalls.incrementAndGet()
        }

        override def iterator: Iterator[Int] = {
          data.iterator
        }
      }

      val createdSources = ListBuffer[TestData]()
      def updateSource(query: QueryRequest): KafkaReactive.Source[Int] = {
        val values   = query.clientId.toInt to query.groupId.toInt
        val instance = new TestData(values)
        createdSources += instance
        instance
      }

      implicit val sched = Scheduler.global

      def newRequest(from: Int, to: Int): QueryRequest = {
        QueryRequest(
          clientId = from.toString,
          groupId = to.toString,
          topic = "doesn't matter",
          filterExpression = "yo",
          fromOffset = None,
          messageLimit = None,
          streamStrategy = StreamStrategy.All
        )
      }

      def allowAll(expression: String) = { _: Int =>
        true
      }
      val reactive                   = KafkaReactive[Int](allowAll, updateSource)
      val underTest: Observable[Int] = reactive.source
      val initialRequest             = newRequest(0, 10)
      reactive.update(UpdateFeedRequest(initialRequest))

      Then("we should receive values from the initial query")
      val firstBatch = (0 to 10).toList
      underTest.take(firstBatch.size).toListL.runSyncUnsafe(testTimeout) shouldBe firstBatch

      createdSources.size shouldBe 1

      When("An updated query is sent")
      reactive.update(UpdateFeedRequest(newRequest(20, 30)))

      Then("The previous source should be closed")
      eventually {
        createdSources.head.closeCalls.get shouldBe 1
      }

      When("Another update is sent")
      val secondBatch = (20 to 30).toList
      reactive.update(UpdateFeedRequest(newRequest(20, 30)))

      And("The source should observe the data from the new feed")
      underTest.take(secondBatch.size).toListL.runSyncUnsafe(testTimeout) shouldBe secondBatch
      createdSources.size shouldBe 2

      And("The old source should be closed")
      createdSources.head.closeCalls.get shouldBe 1
      createdSources.tail.head.closeCalls.get shouldBe 1
    }
  }
  "KafkaReactive.throttle" should {
    // a source of data which will push out elements as quickly as they can be consumed (!!!).
    val lotsOfConsecutiveDataQuickly = Observable.fromIterator(Task.eval(Iterator.from(0)))
    "produce all elements for StreamStrategy.All" in {

      val total = 100

      val reactive: Observable[Int] = KafkaReactive.throttle(lotsOfConsecutiveDataQuickly.take(total), messageLimit = None, StreamStrategy.All)

      val S = TestSubscriber() {
        case (i, s) =>
          // give it a chance to miss some
          Thread.sleep(5)
          1
      }

      reactive.subscribe(S.toMonix)

      S.done.await(testTimeout.toMillis, TimeUnit.MILLISECONDS)
      S.receivedCount shouldBe total

      withClue("no elements should've been skipped") {
        S.receivedToList shouldBe (0 until total).toList
      }
    }
    "only provide the latest elements for StreamStrategy.Latest" in {
      val reactive: Observable[Int] = KafkaReactive.throttle(lotsOfConsecutiveDataQuickly, messageLimit = None, StreamStrategy.Latest)

      val S = TestSubscriber() {
        case (i, s) =>
          // give it a chance to miss some
          Thread.sleep(10)
          1
      }

      reactive.subscribe(S.toMonix)

      eventually {
        withClue("some elements should've been skipped while our 'onNext' was sleeping") {
          val weSkippedSome = S.receivedToList.sliding(2, 1).exists {
            case List(a, b) => b > a + 1
          }
          weSkippedSome shouldBe true
        }
      }
    }

    "produce all elements when no message limit is specified for StreamStrategy.All" ignore {
      val reactive: Observable[Int] = KafkaReactive.throttle(lotsOfConsecutiveDataQuickly, messageLimit = Option(Rate(100, 1.second)), StreamStrategy.Latest)

      val S = TestSubscriber() {
        case (i, s) =>
          println(s"onNext($i)...")
          Thread.sleep(2000)

          1
      }
      reactive.subscribe(S.toMonix)

      println("waiting...")
      S.done.await(10.seconds.toMillis, TimeUnit.MILLISECONDS)
      println("done")
    }
  }
  "KafkaReactive.select" should {
    val input = (1 to 17).toList

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

  object TestSubscriber {

    def apply(done: CountDownLatch = new CountDownLatch(1))(handleNext: (Int, Subscription) => Int) = {
      new TestSubscriber(done, handleNext)
    }
  }

  class TestSubscriber(val done: CountDownLatch = new CountDownLatch(1), handleNext: (Int, Subscription) => Int) extends Subscriber[Int] {

    val received = ListBuffer[Int]()

    val cancellable = Cancelable { () =>
      Option(subscription).foreach(_.cancel)
    }

    def receivedToList = received.toList
    def receivedCount  = received.size

    def toMonix(implicit scheduler: Scheduler = monix.execution.Scheduler.global) = {
      val o = Observer.fromReactiveSubscriber(this, cancellable)
      monix.reactive.observers.Subscriber(o, scheduler)
    }

    var subscription: Subscription = null
    override def onSubscribe(s: Subscription): Unit = {
      require(subscription == null)
      subscription = s
      subscription.request(1)
    }

    override def onNext(next: Int): Unit = {
      received += next
      val nrToRequest = handleNext(next, subscription)
      if (nrToRequest > 0) {
        subscription.request(nrToRequest)
      }
    }

    override def onError(t: Throwable): Unit = {
      println(s"oops: $t")
      subscription.cancel()
    }

    override def onComplete(): Unit = {
      done.countDown()
    }
  }
}
