package pipelines.connect

import java.util
import java.util.Collections

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.{PartitionInfo, TopicPartition}
import pipelines.eval.Provider
import pipelines.kafka.{PartitionData, PullLatestResponse}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.util.Try

/**
  * pimped KafkaConsumer
  *
  * @param client
  * @param defaultPollTimeout
  * @tparam K
  * @tparam V
  */
class RichKafkaConsumer[K, V](val client: KafkaConsumer[K, V], defaultPollTimeout: FiniteDuration)(implicit scheduler: Scheduler)
    extends Provider[Observable[ConsumerRecord[K, V]]]
    with StrictLogging {

  private class Listener(initialTopics: Set[String], offset: String) extends ConsumerRebalanceListener {
    var processedTopics = initialTopics
    import scala.collection.JavaConverters._
    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
      val list = partitions.asScala.toSeq
      logger.info(s"${list.size} partitions revoked: ${list.mkString(",")}")
    }

    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
      val list = partitions.asScala.toSeq
      logger.info(s"${list.size} partitions assigned: ${list.mkString(",")}, processedTopics is $processedTopics, initial is $initialTopics")

      val filtered: Seq[TopicPartition] = list.filter(p => processedTopics.contains(p.topic()))
      if (filtered.nonEmpty) {

        processedTopics = processedTopics -- filtered.map(_.topic())

        offset match {
          case "earliest" =>
            logger.info(s"Seeking to earliest for ${filtered}")
            client.seekToBeginning(filtered.asJava)
          case "latest" =>
            val ok = client.seekToEnd(filtered.asJava)
            logger.info(s"Seeking to latest for ${filtered} returned: $ok")
            client.seekToEnd(filtered.asJava)
          case number =>
            val offsetTry = Try(number.toLong)
            logger.info(s"Seeking to $offsetTry for ${filtered}")
            offsetTry.foreach { offset =>
              filtered.foreach { p =>
                client.seek(p, offset)
              }
            }
        }
      }
    }
  }

  def subscribe(topics: Set[String], fromOffset: Option[String]) = {
    import scala.collection.JavaConverters._
    fromOffset match {
      case None =>
        client.subscribe(topics.asJava)
      case Some(offset) =>
        client.subscribe(topics.asJava, new Listener(topics, offset))

    }
  }

  def pullLatest(topic: String, limit: Long, pollTimeout: FiniteDuration, timeout: FiniteDuration, formatKey: K => String): PullLatestResponse = {
    import scala.collection.JavaConverters._
    client.subscribe(List(topic).asJava)

    val found: Seq[ConsumerRecord[K, V]] = if (limit <= 0) {
      pullOrEmpty(pollTimeout).toSeq
    } else {
      val received  = ArrayBuffer[ConsumerRecord[K, V]]()
      var remaining = limit
      val deadline  = timeout.fromNow
      while (remaining > 0 && deadline.hasTimeLeft) {
        val next: Iterable[ConsumerRecord[K, V]] = pullOrEmpty(pollTimeout)
        val iter                                 = next.iterator
        while (remaining > 0 && iter.hasNext) {
          remaining = remaining - 1
          received += iter.next
        }
        iter.size
      }
      received
    }

    val results = found.map { record =>
      formatKey(record.key)
    }
    PullLatestResponse(topic, results)
  }

  def listTopics(): Map[String, List[PartitionData]] = {
    import scala.collection.JavaConverters._
    client.listTopics.asScala.mapValues { info: util.List[PartitionInfo] =>
      info.asScala.map { pi =>
        PartitionData(partition = pi.partition, leader = Option(pi.leader).fold("")(_.idString))
      }.toList
    }.toMap
  }

  def pull(pollTimeout: FiniteDuration = defaultPollTimeout): Iterator[ConsumerRecord[K, V]] = {
    val all: Iterator[Iterable[ConsumerRecord[K, V]]] = Iterator.continually(pullOrEmpty(pollTimeout))
    all.flatten
  }

  def pullOrEmpty(pollTimeout: FiniteDuration = defaultPollTimeout): Iterable[ConsumerRecord[K, V]] = {
    if (isClosed()) {
      Nil
    } else {
      doPullOrEmpty(pollTimeout)
    }
  }

  private def doPullOrEmpty(pollTimeout: FiniteDuration = defaultPollTimeout): Iterable[ConsumerRecord[K, V]] = {
    val records: ConsumerRecords[K, V] = client.poll(java.time.Duration.ofMillis(pollTimeout.toMillis))

    val partitions = records.partitions()

    val total = records.count
    if (total > 0) {
      logger.debug(s"got ${total} records, ${partitions.size} partitions")
    } else {
      logger.trace(s"got ${total} records, ${partitions.size} partitions")
    }

    import scala.collection.JavaConverters._
    partitions.asScala.flatMap { partition =>
      val partitionRecords = records.records(partition).asScala

      if (partitionRecords.nonEmpty) {
        val total      = partitionRecords.size
        val lastOffset = partitionRecords.last.offset
        logger.debug(s"partition $partition has ${total} records, committing lastOffset $lastOffset + 1")
        client.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)))
        logger.debug(s"partition $partition has ${total} records, committed lastOffset $lastOffset + 1")
      } else {
        logger.debug(s"partition $partition has no records")
      }
      partitionRecords
    }

  }

  private def isClosed() = {
    if (closed) {
      if (!cleanedUp) {
        cleanedUp = true
        logger.info("Actually closing kafka consumer")
        client.unsubscribe()
        client.close()
      }
      true
    } else {
      false
    }
  }

  @volatile private var closed    = false
  @volatile private var cleanedUp = false

  override def close(): Unit = {
    logger.info("Registering closing for kafka consumer")
    // kafka client isn't safe for multi-threaded access
    closed = true
  }

  final def asObservable: Observable[ConsumerRecord[K, V]] = {
    val (input: Observer[ConsumerRecord[K, V]], output) = Pipe.publish[ConsumerRecord[K, V]].multicast
    scheduler.scheduleOnce(defaultPollTimeout) {
      while (true) {
        val iter = pullOrEmpty()
        Observer.feed(input, iter)
        Thread.`yield`()
      }
    }
    output
  }

  final override lazy val data: Observable[ConsumerRecord[K, V]] = asObservable
}

object RichKafkaConsumer extends StrictLogging {

  def apply[K, V](rootConfig: Config, keySerializer: Deserializer[K], valueSerializer: Deserializer[V])(implicit scheduler: Scheduler): RichKafkaConsumer[K, V] = {

    import args4c.implicits._
    val consumerConf: Config = rootConfig.getConfig("pipelines.consumer")
    logger.debug(s"creating new kafka consumer client for:\n${consumerConf.summary()}\n")
    val pollFreq = consumerConf.asFiniteDuration("poll.interval")
    val props    = propertiesForConfig(consumerConf)

    val consumer = new KafkaConsumer[K, V](props, keySerializer, valueSerializer)
    new RichKafkaConsumer[K, V](consumer, pollFreq)
  }

  def byteArrayValues(rootConfig: Config)(implicit scheduler: Scheduler): RichKafkaConsumer[String, Bytes] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArrayDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def strings(rootConfig: Config)(implicit scheduler: Scheduler): RichKafkaConsumer[String, String] = {
    val keySerializer   = new org.apache.kafka.common.serialization.StringDeserializer
    val valueSerializer = new org.apache.kafka.common.serialization.StringDeserializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

}
