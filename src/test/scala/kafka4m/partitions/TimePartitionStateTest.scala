package kafka4m.partitions

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

import kafka4m.BaseKafka4mSpec
import org.scalatest.GivenWhenThen

import scala.concurrent.duration._

class TimePartitionStateTest extends BaseKafka4mSpec with GivenWhenThen {
  import TimeBucketTest._

  "TimePartitionState.update" should {
    "produce a 'flush' event if N records have been observed since the last time we saw a record for a particular time bucket" in {
      Given("A TimePartitionState which will put records into 7 minute buckets and flush buckets after 2 messages are seen in different buckets")
      val initialState = TimePartitionState[ZonedDateTime](recordsReceivedBeforeClosingBucket = 2, timeBucketSize = 7.minutes)
      val index        = new AtomicInteger(0)
      var state        = initialState
      def inc(next: ZonedDateTime): Seq[AppendEvent[ZonedDateTime]] = {
        val (s, r) = state.update(next, index.incrementAndGet)
        state = s
        r
      }

      When("We update the state with our first record which has a timestamp of 5 minutes past")
      val r1 = timeForHourAndMinute(5, 5)

      Then("It should produce an append event with a bucket of 0-7 minutes past")
      inc(r1) shouldBe Seq(AppendData[ZonedDateTime](TimeBucket(5, 0, 7), r1))

      When("We update the state with another record which has a timestamp of 8 minutes past")
      val r2 = timeForHourAndMinute(5, 8)

      Then("It should produce an append event with a bucket of 7-14 minutes past")
      inc(r2) shouldBe Seq(AppendData[ZonedDateTime](TimeBucket(5, 7, 14), r2))

      When("One more message is sent in another bucket")
      val r3 = timeForHourAndMinute(5, 8)

      Then("We should see a flush event for our first message")
      inc(r3) shouldBe Seq(AppendData[ZonedDateTime](TimeBucket(5, 7, 14), r2), FlushBucket(TimeBucket(5, 0, 7)))
    }
  }

}
