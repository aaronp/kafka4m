package pipelines.eval

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{CountDownLatch, TimeUnit}

import pipelines.kafka._
import monix.eval.Task
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, Observer}
import org.reactivestreams.{Subscriber, Subscription}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import pipelines.core.{Rate, StreamStrategy}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class EvalReactiveTest extends WordSpec with Matchers with Eventually with GivenWhenThen with ScalaFutures {

  def testTimeout: FiniteDuration = 3.seconds

  "KafkaReactive.source" should {
    "close and reconnect a new source when a new query request is made" in {
      Given("A queryable data source")
      class TestData(override val data: Observable[Int]) extends Provider[Observable[Int]] {
        val closeCalls = new AtomicInteger(0)
        override def close(): Unit = {
          closeCalls.incrementAndGet()
        }
      }

      val createdSources = ListBuffer[TestData]()
      def updateSource(query: QueryRequest): Provider[Observable[Int]] = {
        val values   = query.clientId.toInt to query.groupId.toInt
        val instance = new TestData(Observable.fromIterable(values))
        createdSources += instance
        instance
      }

      implicit val sched = Scheduler.global

      def newRequest(from: Int, to: Int): QueryRequest = {
        new QueryRequest(
          clientId = from.toString,
          groupId = to.toString,
          topic = "topic",
          filterExpression = "filterExpression",
          filterExpressionIncludeMatches = true,
          fromOffset = Option("latest"),
          messageLimit = Option(Rate.perSecond(123)),
          format = Option(ResponseFormat(List("key", "foo"))),
          streamStrategy = StreamStrategy.Latest
        )
      }

      val reactive                   = EvalReactive[Int](updateSource)
      val underTest: Observable[Int] = reactive.source.map(_._2)
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

      val reactive: Observable[Int] = EvalReactive.throttle(lotsOfConsecutiveDataQuickly.take(total), messageLimit = None, StreamStrategy.All)

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
      val reactive: Observable[Int] = EvalReactive.throttle(lotsOfConsecutiveDataQuickly, messageLimit = None, StreamStrategy.Latest)

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
      val reactive: Observable[Int] = EvalReactive.throttle(lotsOfConsecutiveDataQuickly, messageLimit = Option(Rate(100, 1.second)), StreamStrategy.Latest)

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
      EvalReactive.select(input, 0) should be(empty)
      EvalReactive.select(input, 1) shouldBe Seq(1)
      EvalReactive.select(input, 2) shouldBe Seq(1, 9)
      EvalReactive.select(input, 3) shouldBe Seq(1, 6, 11)
      EvalReactive.select(input, 12) shouldBe (1 to 12)
      EvalReactive.select(input, input.size) shouldBe input
      EvalReactive.select(input, input.size + 1) shouldBe input
      EvalReactive.select(input, input.size + 2) shouldBe input
      EvalReactive.select(input, input.size + 3) shouldBe input
      EvalReactive.select(input, input.size * 2) shouldBe input
      EvalReactive.select(input, Int.MaxValue) shouldBe input
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
      subscription.cancel()
    }

    override def onComplete(): Unit = {
      done.countDown()
    }
  }
}
