package kafka4m.io

import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.Config
import monix.eval.Task
import monix.reactive.Observable

/**
  * A means to get a stream of data from a directory, if only to make kafka4m useful out-of-the-box.
  *
  * This way we can get a stream of data from each file
  */
object FileSource {

  case class EtlConfig(dataDir: String, cache: Boolean, rateLimitPerSecond: Option[Int], limit: Option[Long], repeat: Boolean, fileNamesAsKeys: Boolean)

  object EtlConfig {
    def apply(config: Config): EtlConfig = {
      val etlConfig = config.getConfig("kafka4m.etl.intoKafka")
      new EtlConfig(
        dataDir = etlConfig.getString("dataDir"),
        cache = etlConfig.getBoolean("cache"),
        rateLimitPerSecond = Option(etlConfig.getInt("rateLimitPerSecond")).filter(_ > 0),
        limit = Option(etlConfig.getLong("limit")).filter(_ > 0),
        repeat = etlConfig.getBoolean("repeat"),
        fileNamesAsKeys = etlConfig.getBoolean("fileNamesAsKeys")
      )
    }
  }

  /** @param config the typesafe config
    * @return a stream of file names and content
    */
  def apply(config: Config): Observable[(String, Array[Byte])] = {
    keysAndData(EtlConfig(config))
  }

  def keysAndData(conf: EtlConfig): Observable[(String, Array[Byte])] = {
    val obs = unlimited(conf)
    val limited = conf.limit.fold(obs) { total =>
      obs.take(total)
    }

    conf.rateLimitPerSecond.fold(limited) { perSecond =>
      import concurrent.duration._
      limited.bufferTimedWithPressure(1.second / perSecond, 1).flatMap { buffer =>
        Observable.fromIterable(buffer)
      }
    }
  }

  private def unlimited(conf: EtlConfig): Observable[(String, Array[Byte])] = {
    val dir = Paths.get(conf.dataDir)
    def all: Observable[(String, Array[Byte])] = {
      if (conf.cache) {
        val data: List[(String, Array[Byte])] = cacheDirContents(dir)
        if (conf.repeat) {
          Observable.fromIterable(data).repeat
        } else {
          Observable.fromIterable(data)
        }
      } else {
        listChildrenObservable(dir, conf.repeat).map { file =>
          file.getFileName.toString -> Files.readAllBytes(file)
        }
      }
    }

    if (conf.repeat) {
      val LastDot = "(.*?)\\.(.*)".r

      all.zipWithIndex.map {
        case ((name, data), i) if conf.fileNamesAsKeys =>
          val newName = name match {
            case LastDot(prefix, suffix) => s"${prefix}-$i.$suffix"
            case name                    => s"${name}-$i"
          }
          (newName, data)
        case ((_, data), i) => (i.toString, data)
      }
    } else {
      all
    }
  }

  private[io] def cacheDirContents(dir: Path): List[(String, Array[Byte])] = {
    listChildren(dir).toList.map { path =>
      (path.getFileName.toString, Files.readAllBytes(path))
    }
  }

  private[io] def listChildrenObservable(dir: Path, repeat: Boolean): Observable[Path] = {
    val children: Task[Iterator[Path]] = Task.delay(listChildren(dir))
    if (repeat) {
      Observable.repeatEval(children).flatMap { task: Task[Iterator[Path]] =>
        Observable.fromIterator(task)
      }
    } else {
      Observable.fromIterator(children)
    }
  }

  def listChildren(dir: Path): Iterator[Path] = {
    import scala.collection.JavaConverters._
    Files.walk(dir).iterator.asScala.filter(p => Files.isRegularFile(p))
  }
}
