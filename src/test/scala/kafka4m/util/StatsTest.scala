package kafka4m.util

import com.typesafe.config.ConfigFactory
import kafka4m.BaseKafka4mSpec
import org.apache.kafka.clients.consumer.ConsumerRecord

class StatsTest extends BaseKafka4mSpec {
  import eie.io._

  "Stats" should {
    "create a config txt file on creation" in {
      withTmpDir { dir =>
        val config = ConfigFactory.parseString(s"""kafka4m {
            |  etl.stats.writeTo : "${dir.toAbsolutePath}"
            |}""".stripMargin).withFallback(ConfigFactory.load())
        val stats  = Stats(config)

        eventually {
          dir.resolve("config.txt").isFile shouldBe true
        }
        val confLine = dir.resolve("config.txt").lines.find(_.contains(s"kafka4m.etl.stats.writeTo : ${dir.toAbsolutePath}"))
        confLine.isDefined shouldBe true
      }
    }
  }
  "Stats.flushHtml" should {
    "produce an index.html" in {
      import eie.io._
      withTmpDir { dir =>
        val config = ConfigFactory.parseString(s"""kafka4m {
            |  etl.stats.writeTo : "${dir.toAbsolutePath}"
            |}""".stripMargin).withFallback(ConfigFactory.load())
        val stats  = Stats(config)

        eventually {
          dir.resolve("config.txt").isFile shouldBe true
        }

        Schedulers.using { s =>
          def incWrites() = stats.onWriteToKafka.runToFuture(s).futureValue
          (0 to 100).foreach(_ => incWrites())
          (0 to 100).foreach { i =>
            val record = new ConsumerRecord("topic", i % 3, i, "key", "value")
            stats.onReadFromKafka(record).runToFuture(s).futureValue
          }
        }

        stats.flush()
        val confLine = dir.resolve("config.txt").lines.find(_.contains(s"kafka4m.etl.stats.writeTo : ${dir.toAbsolutePath}"))
        confLine.isDefined shouldBe true
      }
    }
  }
}
