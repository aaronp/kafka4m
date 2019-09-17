package kafka4m.io

import com.typesafe.scalalogging.StrictLogging
import eie.io._
import kafka4m.BaseKafka4mSpec
import kafka4m.util.Schedulers
import monix.eval.Task
import monix.execution.Cancelable
import monix.execution.cancelables.BooleanCancelable
import monix.reactive.Observable

import scala.collection.mutable.ListBuffer

class FileSinkTest extends BaseKafka4mSpec with StrictLogging {

  "FileSink.base64" should {
    "write to a zip file quickly" in {

      val iter = Iterator.from(0).map { i =>
        s"${i}:some data"
      }
      val input = Observable.fromIterator(Task(iter))
      var count = 0
      val inc = Task.eval {
        count = count + 1
      }
      withTmpDir { testDir =>
        val textFile = testDir.resolve("file.txt")
        import cats.instances.string._
        val sink = FileSink.text[String](textFile, flushEvery = 1000)

        Schedulers.using { s =>
          import concurrent.duration._
          var lastCount                = 0
          val throughputObserved       = ListBuffer[Int]()
          val expectedRecordsPerSecond = 200000
          s.scheduleAtFixedRate(1.second, 1.second) {
            val throughput = count - lastCount
            logger.info(s"$throughput / second")
            throughputObserved += throughput
            lastCount = count
          }
          val stop         = BooleanCancelable()
          var isDone       = false
          val noteWhenDone = Task.delay[Unit] { isDone = true }
          val task: Cancelable = {
            val testData: Observable[String] = input.doOnNext(_ => inc).takeWhileNotCanceled(stop).guarantee(noteWhenDone)
            testData.subscribe(sink)(s)
          }

          eventually {
            throughputObserved.exists(_ > expectedRecordsPerSecond) shouldBe true
          }
          stop.cancel()
          eventually {
            sink.isClosed() shouldBe true
          }
          task.cancel()
          isDone shouldBe true
          sink.size() shouldBe count - 1
          textFile.lines.next() shouldBe s"0:some data"
        }
      }

    }
    "write to a zip file" in {
      val timestamp = System.currentTimeMillis
      withTmpDir { testDir =>
        withTmpDir { unzipDir =>
          val zipFile = testDir.resolve("file.zip")
          val sink    = FileSink.zipped(zipFile, flushEvery = 1)
          sink.onNext("foo" -> "foo data".getBytes()).futureValue
          sink.onNext("bar" -> "bar data".getBytes()).futureValue
          sink.onComplete()

          Unzip.to(zipFile, unzipDir)
          unzipDir.children.map(f => f.fileName -> f.text).toMap shouldBe Map("foo" -> "foo data", "bar" -> "bar data")

        }
      }
    }
  }
}
