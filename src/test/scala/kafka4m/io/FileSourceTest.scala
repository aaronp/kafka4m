package kafka4m.io

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import eie.io._
import kafka4m.BaseKafka4mSpec
import kafka4m.io.FileSource.EtlConfig
import kafka4m.util.Schedulers
import monix.execution.Scheduler
import monix.reactive.Observable

class FileSourceTest extends BaseKafka4mSpec {
  implicit def asRichObs[A](obs: Observable[A])(implicit s: Scheduler) = new {
    def takeList(n: Int): List[A] = {
      obs.take(n).toListL.runToFuture(s).futureValue
    }
  }

  "FileSource.keysAndData" should {
    "pick up newly created files which appear in the directory" in {
      withTmpDir { dir =>
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
