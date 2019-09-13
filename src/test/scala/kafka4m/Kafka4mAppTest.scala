package kafka4m

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.util.UUID

import com.typesafe.config.ConfigFactory
import kafka4m.io.{Base64Writer, TextAppenderObserver}
import kafka4m.partitions.{AppendEvent, ForceFlushBuckets, TimeBucket}
import kafka4m.util.Schedulers
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord

class Kafka4mAppTest extends BaseKafka4mDockerSpec {
  "Kafka4mApp.writeToKafka" should {
    "write and read data from kafka" in {
      val originalTopic = UUID.randomUUID.toString.filter(_.isLetterOrDigit)
      Given(s"A configuration with a new topic '$originalTopic'")
      val conf1 = Kafka4mAppTest.testConfig(originalTopic)

      Schedulers.using { s =>
        When("We write some test data into kafka for that topic")
        val (report, task)   = Kafka4mApp.writeToKafka(conf1)(s)
        val numWritten: Long = task.futureValue
        report.cancel()
        numWritten shouldBe 4L

        Then("We should be able to read out the data from that topic")
        val bucketWrites: Observable[(TimeBucket, Path)] = {
          val writer                                                          = Base64Writer(conf1)
          val kafkaData                                                       = kafka4m.read(conf1).dump("from kafka").take(numWritten)
          val readEvents: Observable[AppendEvent[ConsumerRecord[Key, Bytes]]] = writer.asEvents(kafkaData).dump("\treadEvents")
          writer.write(readEvents :+ ForceFlushBuckets[ConsumerRecord[Key, Bytes]]())
        }
        val bucketsAndPaths: List[(TimeBucket, Path)] = bucketWrites.toListL.runToFuture(s).futureValue
        import eie.io._

        val writtenLines = bucketsAndPaths.flatMap(_._2.text.linesIterator.collect {
          case TextAppenderObserver.Base64Line(key, contentBytes) =>
            new String(contentBytes, StandardCharsets.UTF_8)
        })

        writtenLines should contain("The first file")
        writtenLines should contain("Another file")
        writtenLines.flatMap(_.linesIterator) should contain("This file is considerably larger")
      }
    }
  }

}
object Kafka4mAppTest {
  def testConfig(topic: String) = {
    val etlConfFile = getClass.getClassLoader.getResource("kafka4mapp-test-data/test-etl.conf")
    require(
      etlConfFile != null,
      "our test data file has moved or doesn't exist! Or somehow we're running this test from a jar file. Or java's broken. Or I introduced a bug"
    )

    val file   = Paths.get(etlConfFile.toURI)
    val config = ConfigFactory.parseFile(file.toFile)

    val custom = ConfigFactory.parseString(s"""
                                              |kafka4m.topic : ${topic}
                                              |kafka4m.consumer.group.id : group${topic}
         |kafka4m.etl.intoKafka.dataDir : ${file.getParent.toAbsolutePath.toString}
         |kafka4m.etl.fromKafka.dataDir : ./target/Kafka4mAppTest/${topic}
         |""".stripMargin)

    custom.withFallback(config).withFallback(ConfigFactory.load())
  }
}
