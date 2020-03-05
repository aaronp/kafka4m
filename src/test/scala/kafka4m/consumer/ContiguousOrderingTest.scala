package kafka4m.consumer

import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class ContiguousOrderingTest extends AnyWordSpec with Matchers {
  val testTimeout = 2.seconds

  "LongOrdered" should {
    List(
      List()                       -> List(),
      List(1, 3, 4, 5, 2, 6, 8, 9) -> List(1, 2, 3, 4, 5, 6), // wait for seven
      List(1, 3, 2)                -> List(1, 2, 3)
    ).foreach {
      case (input, expected) =>
        input.mkString("sort [", ",", s"] as ${expected.mkString("[", ",", "]")}") in {
          val actual = ContiguousOrdering.sort(Observable.fromIterable(input)).toListL.runSyncUnsafe(testTimeout)
          actual shouldBe expected
        }
    }
  }
}
