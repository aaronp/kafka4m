package kafka4m.partitions

import java.time.ZonedDateTime

import dockerenv.BaseKafkaSpec
import org.scalatest.FunSuite

class TimeBucketTest extends BaseKafkaSpec {
  import TimeBucketTest._
  "TimeBucket" should {
    "partition epochs into 20 minute buckets" in {
      TimeBucket(20, timeForHourAndMinute(5, 0)) shouldBe TimeBucket(5, 0, 20)
      TimeBucket(20, timeForHourAndMinute(5, 19)) shouldBe TimeBucket(5, 0, 20)
      TimeBucket(20, timeForHourAndMinute(5, 20)) shouldBe TimeBucket(5, 20, 40)
      TimeBucket(20, timeForHourAndMinute(5, 30)) shouldBe TimeBucket(5, 20, 40)
      TimeBucket(20, timeForHourAndMinute(5, 40)) shouldBe TimeBucket(5, 40, 60)

      TimeBucket(20, timeForHourAndMinute(5, 0).minusNanos(1)) shouldBe TimeBucket(4, 40, 60)
      TimeBucket(20, timeForHourAndMinute(5, 20).plusNanos(1)) shouldBe TimeBucket(5, 20, 40)
    }
  }
}

object TimeBucketTest {

  def timeForHourAndMinute(hr: Int, min: Int): ZonedDateTime = {
    ZonedDateTime.of(2019, 2, 3, hr, min, 0, 0, UTC)
  }
}
