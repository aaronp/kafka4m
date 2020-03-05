package kafka4m

import java.nio.file.Path

import args4c.ConfigApp
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.io.{Base64Writer, FileSource}
import kafka4m.partitions.TimeBucket
import kafka4m.util.{Schedulers, Stats}
import monix.execution.{Cancelable, CancelableFuture, Scheduler}
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

/**
  * An ETL entry point to read data into or out of kafka
  */
object Kafka4mApp extends ConfigApp with StrictLogging {
  override type Result = Cancelable

  // a noddy little hack. the first argument to our main method is an action, which
  // we set in the global space here and then invoke in
  private var action: Action = null

  override def main(args: Array[String]) = {
    if (mainDelegate(args).isEmpty) {
      println(s"Usage: Kafka4m [read | write] (args...)")
      sys.exit(1)
    }
  }

  def mainDelegate(args: Array[String]): Option[Cancelable] = {
    args.headOption match {
      case Some("read") =>
        action = Read
        runMain(args.tail)
      case Some("write") =>
        action = Write
        runMain(args.tail)
      case _ => None
    }
  }

  def summary(config: Config): String = {
    config
      .getConfig("kafka4m")
      .summary()
      .linesIterator
      .map { line =>
        s"\tkafka4m.${line}"
      }
      .mkString("\n")
  }
  def startupLog(action: String, config: Config): String = {
    val kafka4mConf = summary(config)
    s"Running $action with: \n${kafka4mConf}\n\n"
  }

  private sealed trait Action {
    def run(config: Config): Cancelable
  }
  private case object Read extends Action {
    override def run(config: Config): Cancelable = {
      val s                = Schedulers.io()
      val (reportC, paths) = readFromKafka(config)(s)
      val future = paths.foreach {
        case (bucket, dir) =>
          logger.info(s"Wrote $bucket to $dir")
      }(s)
      Cancelable.collection(reportC, future)
    }
  }
  private case object Write extends Action {
    override def run(config: Config): Cancelable = {
      val s              = Schedulers.io()
      val (reportC, fut) = writeToKafka(config)(s)
      Cancelable.collection(reportC, fut)
    }
  }

  /** Read data from kafka to a local disk
    * @param config the kafka4m root configuration
    * @param scheduler
    * @return an observable of the buckets and paths written
    */
  def readFromKafka(config: Config)(implicit scheduler: Scheduler): (Cancelable, Observable[(TimeBucket, Path)]) = {
    val stats = Stats(config)

    val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = {
      if (stats.enabled) {
        kafka4m.readRecords[ConsumerRecord[Key, Bytes]](config).doOnNext(stats.onReadFromKafka)
      } else {
        kafka4m.readRecords[ConsumerRecord[Key, Bytes]](config)
      }
    }

    val reporter = stats.start(scheduler)
    val future   = Base64Writer(config).partition(kafkaData)
    (reporter, future)
  }

  /** write data into kafka using the 'kafka4m.etl.intoKafka' config entry
    * @param config the root configuration
    * @param scheduler
    * @return
    */
  def writeToKafka(config: Config)(implicit scheduler: Scheduler): (Cancelable, CancelableFuture[Long]) = {
    val stats = Stats(config)

    val data: Observable[(String, Array[Byte])] = if (stats.enabled) {
      FileSource(config).doOnNext(_ => stats.onWriteToKafka)
    } else {
      FileSource(config)
    }
    val future = writeToKafka(config, data)(scheduler)

    val reporter = stats.start(scheduler)
    (reporter, future)
  }

  def writeToKafka(config: Config, data: Observable[(String, Array[Byte])])(implicit scheduler: Scheduler): CancelableFuture[Long] = {
    val writer: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)
    data.consumeWith(writer).runToFuture
  }

  override def run(config: Config): Cancelable = {
    require(action != null, "Hack: This should really only be run via the 'main' entry point.")
    logger.info(startupLog(action.toString, config))
    action.run(config)
  }
}
