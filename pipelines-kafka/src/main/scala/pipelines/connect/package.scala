package pipelines

import java.util.Properties
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import monix.reactive.Observable
import org.apache.kafka.clients.consumer.ConsumerRecord
import pipelines.eval.AvroReader
import pipelines.eval.EvalReactive.ReaderLookup
import pipelines.expressions.AvroExpressions.Predicate
import pipelines.expressions.{AvroExpressions, Cache}
import pipelines.kafka.QueryRequest

import scala.util.{Failure, Success, Try}

package object connect extends LazyLogging {

  type Bytes = Array[Byte]

  def format(all: Properties): String = {
    import scala.collection.JavaConverters._
    all
      .keySet()
      .asScala
      .map(_.toString)
      .map { key =>
        val value = all.getProperty(key)
        s"${key} : ${value}"
      }
      .mkString(";\n")
  }

  def propertiesForConfig(config: Config): Properties = {
    import args4c.implicits._
    config
      .collectAsStrings()
      .foldLeft(new java.util.Properties) {
        case (props, (key, AsInteger(value))) =>
          props.put(key, value)
          props
        case (props, (key, value)) =>
          props.put(key, value)
          props
      }
  }

  private object AsInteger {

    def unapply(str: String): Option[Integer] = {
      Try(Integer.valueOf(str.trim)).toOption
    }
  }

  def newSchedulerService(): ScheduledExecutorService = {
    Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r)
        t.setName("flush-scheduler")
        t.setDaemon(true)
        t
      }
    })
  }

  def avroToJsonFormatter(readerForTopic: ReaderLookup, query: QueryRequest, record: ConsumerRecord[String, Bytes]): Observable[String] = {
    import io.circe.syntax._
    avroToRecordJsonFormatter(readerForTopic)(query, record).map(_.asJson.noSpaces)
  }

  private val AvroExpressionsCache: Cache[Predicate] = AvroExpressions.newCache

  def avroRecordResultAsRecordJson(record: ConsumerRecord[String, Bytes], result: Try[DynamicAvroRecord], query: QueryRequest): Observable[RecordJson] = {
    result match {
      case Success(avro: DynamicAvroRecord) =>
        val predicate: AvroExpressions.Predicate = AvroExpressionsCache(query.filterExpression).get
        if (predicate(avro.underlyingRecord) == query.filterExpressionIncludeMatches) {
          Observable(RecordJson(record, record.key, avro.toString))
        } else {
          logger.debug(s"Skipping ${record}")
          Observable.empty
        }
      case Failure(err) =>
        logger.debug(s"Error reading ${record.key} at offset ${record.offset()} : ${err}")
        Observable.empty
    }
  }

  def avroRecordResultAsBinary(record: ConsumerRecord[String, Bytes], result: Try[DynamicAvroRecord], query: QueryRequest): Observable[Bytes] = {
    result match {
      case Success(avro: DynamicAvroRecord) =>
        val predicate = AvroExpressionsCache(query.filterExpression).get
        if (predicate(avro.underlyingRecord) == query.filterExpressionIncludeMatches) {
          Observable(record.value())
        } else {
          logger.debug(s"Skipping ${record}")
          Observable.empty
        }
      case Failure(err) =>
        logger.debug(s"Error reading ${record.key} at offset ${record.offset()} : ${err}")
        Observable.empty
    }
  }

  def avroToRecordJsonFormatter(readerForTopic: ReaderLookup)(query: QueryRequest, record: ConsumerRecord[String, Bytes]): Observable[RecordJson] = {
    readerForTopic(query.topic) match {
      case None => Observable(RecordJson(record, record.key.toString, s"${record.value.length} bytes"))
      case Some(reader: AvroReader[DynamicAvroRecord]) =>
        reader.read(record.value) match {
          case Success(avro: DynamicAvroRecord) =>
            val predicate = AvroExpressionsCache(query.filterExpression).get
            if (predicate(avro.underlyingRecord) == query.filterExpressionIncludeMatches) {
              Observable(RecordJson(record, record.key, avro.toString))
            } else {
              logger.debug(s"Skipping ${record}")
              Observable.empty
            }
          case Failure(err) =>
            logger.debug(s"Error reading ${record.key} at offset ${record.offset()} : ${err}")
            Observable.empty
        }
    }
  }
}
