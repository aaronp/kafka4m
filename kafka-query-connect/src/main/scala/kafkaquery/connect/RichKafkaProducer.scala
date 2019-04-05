package kafkaquery.connect

import java.lang.management.ManagementFactory
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit}

import args4c.implicits._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import javax.management.ObjectName
import RichKafkaProducer.KafkaPublisher
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.Serializer

import scala.concurrent.duration.FiniteDuration
import scala.sys.ShutdownHookThread
import scala.util.Try

class RichKafkaProducer[K, V] private (rootConfig: Config, name: ObjectName, newProducer: Properties => KafkaProducer[K, V])(implicit scheduler: ScheduledExecutorService)
    extends AutoCloseable with RichKafkaProducerMBean with StrictLogging {

  private val kafkaConfig                                = rootConfig.getConfig("kafkaquery.producer")
  private var publisherOpt: Option[KafkaPublisher[K, V]] = None

  private var flushFuture: ScheduledFuture[_] = scheduleFlushes()

  private def defaultFrequency = kafkaConfig.asFiniteDuration("flushEvery")

  private def scheduleFlushes(frequency: FiniteDuration = defaultFrequency) = {
    scheduler.scheduleAtFixedRate(() => flush(), frequency.toMillis, frequency.toMillis, TimeUnit.MILLISECONDS)
  }

  private object Lock

  def publisher(): KafkaPublisher[K, V] = {
    Lock.synchronized {
      publisherOpt.getOrElse {
        publisherOpt = Option(createPublisher())
        publisher()
      }
    }
  }

  private def kafkaProperties(): Properties = {
    val props: Properties = propertiesForConfig(kafkaConfig.withoutPath("flushEvery"))
    propertyOverrides.foldLeft(props) {
      case (p, (k, v)) =>
        p.setProperty(k, v)
        p
    }
  }

  private def createPublisher(): KafkaPublisher[K, V] = {
    val props: Properties = kafkaProperties()
    logger.info(s"Connecting to Kafka using:\n${format(props)}\n")

    val producer: KafkaProducer[K, V] = newProducer(props)
    new KafkaPublisher[K, V](producer)
  }

  def send(kafkaTopic: String, key: K, value: V): Unit = {
    publisher().push(kafkaTopic, key, value)
  }

  override def close(): Unit = {
    logger.info("Closing")
    Try(ManagementFactory.getPlatformMBeanServer().unregisterMBean(name))

    disconnectFromKafka()
  }

  private var propertyOverrides = Map[String, String]()

  override def setProperty(key: String, value: String): String = {
    propertyOverrides = propertyOverrides.updated(key, value)
    getProperties()
  }

  override def getProperties(): String = format(kafkaProperties())

  override def flush() = {
    Lock.synchronized {
      publisherOpt.fold(false) { p =>
        p.flush()
        true
      }
    }
  }

  override def disconnectFromKafka() = {
    Lock.synchronized {
      publisherOpt.fold(false) { p =>
        Try(p.flush())
        p.close()
        publisherOpt = None
        true
      }
    }
  }

  override def reconnect(): Unit = {
    disconnectFromKafka()
    publisher()
    ()
  }

  override def cleanPropertyOverrides(): String = {
    propertyOverrides = Map.empty
    getProperties()
  }

  override def getConfig(): String = {
    rootConfig.getConfig("kafkaquery").summaryEntries().mkString(";\n")
  }

  override def cancelAutoFlush(): Boolean = {
    flushFuture.isCancelled() || flushFuture.cancel(false)
  }

  override def sendTestMessage(topic: String, key: String, value: String) = {
    send(topic, key.asInstanceOf[K], value.asInstanceOf[V])
  }

  override def setAutoFlush(frequencyInMillis: Int): Boolean = {
    val cancelled = cancelAutoFlush()
    import concurrent.duration._
    val freq = if (frequencyInMillis == 0) frequencyInMillis.millis else defaultFrequency
    flushFuture = scheduleFlushes(freq)
    cancelled
  }
}

object RichKafkaProducer {

  class KafkaPublisher[K, V](producer: KafkaProducer[K, V]) extends StrictLogging with AutoCloseable {

    def push(kafkaTopic: String, key: K, value: V): Unit = {
      dirty = true
      logger.debug(s"Sending to Kafka '$kafkaTopic' w/ key $key")
      producer.send(new ProducerRecord[K, V](kafkaTopic, key, value))
    }

    @volatile private var dirty = false

    def flush(): Unit = {
      if (dirty) {
        logger.trace("flushing")
        dirty = false
        producer.flush()
      } else {
        logger.trace("skipping flush")
      }
    }

    override def close(): Unit = producer.close()
  }

  private val globalId = new AtomicInteger(0)

  private def objectName(): ObjectName = {
    new ObjectName(s"kafkaquery:name=Publisher-${globalId.incrementAndGet}")
  }

  def byteArrayValues(rootConfig: Config)(implicit scheduler: ScheduledExecutorService): RichKafkaProducer[String, Bytes] = {
    implicit val keySerializer   = new org.apache.kafka.common.serialization.StringSerializer
    implicit val valueSerializer = new org.apache.kafka.common.serialization.ByteArraySerializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def strings(rootConfig: Config)(implicit scheduler: ScheduledExecutorService): RichKafkaProducer[String, String] = {
    val keySerializer   = new org.apache.kafka.common.serialization.StringSerializer
    val valueSerializer = new org.apache.kafka.common.serialization.StringSerializer
    apply(rootConfig, keySerializer, valueSerializer)
  }

  def apply[K, V](rootConfig: Config, keySerializer: Serializer[K], valueSerializer: Serializer[V])(implicit scheduler: ScheduledExecutorService): RichKafkaProducer[K, V] = {
    val name = objectName()

    def create(props: Properties) = {
      new KafkaProducer[K, V](props, keySerializer, valueSerializer)
    }

    val producer = new RichKafkaProducer[K, V](rootConfig, name, props => create(props))
    ManagementFactory.getPlatformMBeanServer().registerMBean(producer, name)

    ShutdownHookThread {
      producer.close()
    }

    producer
  }

}
