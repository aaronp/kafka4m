package kafka4m

import java.nio.file.Path

import args4c.ConfigApp
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.io.{Base64Writer, FileSource}
import kafka4m.partitions.TimeBucket
import kafka4m.util.{Metrics, Schedulers}
import monix.execution.{Cancelable, CancelableFuture, Scheduler}
import monix.reactive.{Consumer, Observable}

/**
  * An ETL entry point to read data into or out of kafka
  */
object Kafka4mApp extends ConfigApp with StrictLogging {
  override type Result = Unit

  // a noddy little hack. the first argument to our main method is an action, which
  // we set in the global space here and then invoke in
  private var action: Action = null
  override def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some("read") =>
        action = Read
        runMain(args.tail)
      case Some("write") =>
        action = Write
        runMain(args.tail)
      case _ =>
        println(s"Usage: Kafka4m [read | write] (args...)")
        sys.exit(1)
    }
  }

  def startupLog(action: String, config: Config): String = {
    val kafka4mConf = config
      .getConfig("kafka4m")
      .summary()
      .linesIterator
      .map { line =>
        s"\tkafka4m.${line}"
      }
      .mkString("\n")
    s"Running $action with: \n${kafka4mConf}\n\n"
  }

  private sealed trait Action {
    def run(config: Config)
  }
  private case object Read extends Action {
    override def run(config: Config): Unit = {
      val s     = Schedulers.io()
      val paths = readFromKafka(config)(s)
      paths.foreach {
        case (bucket, dir) =>
          logger.info(s"Wrote $bucket to $dir")
      }(s)
    }
  }
  private case object Write extends Action {

    override def run(config: Config): Unit = {
      val s      = Schedulers.io()
      val future = writeToKafka(config)(s)
      println("Running")
    }
  }

  def readFromKafka(config: Config)(implicit scheduler: Scheduler): Observable[(TimeBucket, Path)] = {
    Base64Writer(config).partition(kafka4m.read(config))
  }

  def reportThroughput(perSecond: Int, total: Long): Unit = {
    logger.info(s"$perSecond / second, $total total")
  }

  def writeToKafka(config: Config)(implicit scheduler: Scheduler): (Cancelable, CancelableFuture[Long]) = {
    val data: Observable[(String, Array[Byte])] = FileSource(config)

    val metrics = new Metrics

    val future = writeToKafka(config, data.doOnNext(_ => metrics.incThroughput))(scheduler)

    val reporter = metrics.start(scheduler)

    (reporter, future)
  }

  def writeToKafka(config: Config, data: Observable[(String, Array[Byte])])(implicit scheduler: Scheduler): CancelableFuture[Long] = {
    val writer: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)
    data.consumeWith(writer).runToFuture
  }

  override def run(config: Config) = {
    require(action != null, "Hack: This should really only be run via the 'main' entry point.")
    logger.info(startupLog(action.toString, config))
    action.run(config)
  }
}
