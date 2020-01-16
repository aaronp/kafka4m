package kafka4m.jmx

import kafka4m.BaseKafka4mSpec

class ThroughputTest extends BaseKafka4mSpec {

  "Throughput" should {
    "calculate msgs per second" in {
      val builder = new Throughput.Builder
      builder.inc()
      builder.inc()
      builder.inc()
      val tp1 = builder.flush(1000)
      tp1.meanMessagesPerSecond shouldBe 3
      tp1.latestMessageCount shouldBe 3
      tp1.elapsedMillisSinceLastFlush shouldBe 1000
      builder.inc()
      val tp2 = builder.flush(1500)
      tp2.latestMessageCount shouldBe 1
      tp2.elapsedMillisSinceLastFlush shouldBe 500
      tp2.meanMessagesPerSecond shouldBe 2
      tp2.totalMessages shouldBe 4

      withClue("no flush should occur") {
        val tp3 = builder.flush(1500)
        tp3.meanMessagesPerSecond shouldBe 2
        tp3.elapsedMillisSinceLastFlush shouldBe 500
        tp3.totalMessages shouldBe 4
      }
    }
  }
}
