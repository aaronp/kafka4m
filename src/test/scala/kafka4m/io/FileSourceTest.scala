package kafka4m.io

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import com.typesafe.config.ConfigFactory
import kafka4m.BaseKafka4mSpec
import kafka4m.io.FileSource.EtlConfig
import kafka4m.util.Schedulers
import monix.execution.Scheduler
import monix.execution.ExecutionModel._
import monix.reactive.Observable

class FileSourceTest extends BaseKafka4mSpec {

  import eie.io._

  implicit def asRichObs[A](obs: Observable[A])(implicit s: Scheduler) = new {
    def takeList(n: Int): List[A] = {
      obs.take(n).toListL.runToFuture(s).futureValue
    }
  }

  "FileSource.keysAndData" should {
    "pick up newly created files which appear in the directory" in {
      withTmpDir { dir =>
        val conf = ConfigFactory.parseString(s"""kafka4m.etl.intoKafka {
             |  dataDir : "${dir.toAbsolutePath}"
             |  repeat : false
             |}""".stripMargin).withFallback(ConfigFactory.load())

        val fs = FileSource(conf)

        Schedulers.using(Schedulers.compute(executionModel = AlwaysAsyncExecution)) { implicit s =>
          val three = fs.take(3).toListL.runToFuture

          dir.resolve("a.txt").text = "hi"
          dir.resolve("b.txt").text = "2"
          dir.resolve("c.txt").text = "three"

          val files = three.futureValue
          files.map(_._1) should contain only ("a.txt", "b.txt", "c.txt")
        }
      }
    }
    "repeat cached data" in withTestDir { dir =>
      val data: Observable[(String, Array[Byte])] = FileSource.keysAndData(
        EtlConfig(dir.toAbsolutePath.toString, cache = true, rateLimitPerSecond = None, limit = None, repeat = true, fileNamesAsKeys = true)
      )

      Schedulers.using { implicit s =>
        val actual = data.takeList(6).map {
          case (name, bytes) => name -> new String(bytes, StandardCharsets.UTF_8)
        }
        actual.toMap shouldBe Map(
          ("file2-0.txt", "world"),
          ("file1-1.txt", "hello"),
          ("file2-2.txt", "world"),
          ("file1-3.txt", "hello"),
          ("file2-4.txt", "world"),
          ("file1-5.txt", "hello")
        )
      }
    }
    "repeat non-cached data " in withTestDir { dir =>
      val data: Observable[(String, Array[Byte])] = FileSource.keysAndData(
        EtlConfig(dir.toAbsolutePath.toString, cache = false, rateLimitPerSecond = None, limit = None, repeat = true, fileNamesAsKeys = true)
      )

      Schedulers.using { implicit s =>
        val actual = data.takeList(6).map {
          case (name, bytes) => name -> new String(bytes, StandardCharsets.UTF_8)
        }

        actual.toMap shouldBe Map(
          ("file2-0.txt", "world"),
          ("file1-1.txt", "hello"),
          ("file2-2.txt", "world"),
          ("file1-3.txt", "hello"),
          ("file2-4.txt", "world"),
          ("file1-5.txt", "hello")
        )
      }
    }
    "honor the 'fileNamesAsKeys' setting" in withTestDir { dir =>
      val data: Observable[(String, Array[Byte])] = FileSource.keysAndData(
        EtlConfig(dir.toAbsolutePath.toString, cache = false, rateLimitPerSecond = None, limit = None, repeat = true, fileNamesAsKeys = false)
      )

      Schedulers.using { implicit s =>
        val actual = data.takeList(6).map {
          case (name, bytes) => name -> new String(bytes, StandardCharsets.UTF_8)
        }
        actual.toMap shouldBe Map(
          ("0", "world"),
          ("1", "hello"),
          ("2", "world"),
          ("3", "hello"),
          ("4", "world"),
          ("5", "hello")
        )
      }
    }
  }

  "FileSource.listChildrenObservable" should {
    "be able to repeat the contents" in withTestDir { dir =>
      Schedulers.using { implicit s =>
        val data = FileSource.listChildrenObservable(dir, true)
        data.takeList(4).map(_.fileName) should contain theSameElementsAs List("file1.txt", "file2.txt", "file1.txt", "file2.txt")
      }
    }
  }

  def withTestDir(f: Path => Unit) = {
    import eie.io._
    withTmpDir { dir =>
      dir.resolve("file1.txt").text = "hello"
      dir.resolve("file2.txt").text = "world"
      f(dir)
    }

  }
}
