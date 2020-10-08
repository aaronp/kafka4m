package kafka4m.admin

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.util.{Props, Using}
import monix.execution.{Cancelable, CancelableFuture}
import org.apache.kafka.clients.admin._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.{KafkaFuture, TopicPartition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * A wrapper onto the admin API
  *
  * @param admin the underlying kafka client
  */
final class RichKafkaAdmin(val admin: AdminClient) extends AutoCloseable with StrictLogging {

  @volatile private var closed = false

  /**
    * @param topic             the topic
    * @param numPartitions     the number of partitions
    * @param replicationFactor the replication factor to use when the topic does not exist
    * @param ec
    * @return a future (async result) of an option which will be None if the topic already exists and Some(topic) if it was created
    */
  def getOrCreateTopic(topic: String, numPartitions: Int, replicationFactor: Short, timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[Option[String]] = {
    topics().map { topicsByName: Map[String, TopicListing] =>
      if (!topicsByName.contains(topic)) {
        createTopicBlocking(topic, numPartitions, replicationFactor, timeout)
        Option(topic)
      } else {
        None
      }
    }
  }

  def createTopicBlocking(topic: String, numPartitions: Int, replicationFactor: Short, timeout: FiniteDuration): Unit = {
    val jFuture = createTopic(topic, numPartitions, replicationFactor).all()
    jFuture.get(timeout.toMillis, TimeUnit.MILLISECONDS)
  }

  def createTopicSync(name: String, timeout: FiniteDuration): Unit = {
    val fut: KafkaFuture[Void] = createTopic(name).values().get(name)
    fut.get(timeout.toMillis, TimeUnit.MILLISECONDS)
  }

  def createTopic(name: String, numPartitions: Int = 1, replicationFactor: Short = 1): CreateTopicsResult = {
    createTopic(new NewTopic(name, numPartitions, replicationFactor))
  }

  def createTopic(topic: NewTopic): CreateTopicsResult = {
    admin.createTopics(java.util.Collections.singletonList(topic))
  }

  def metrics = admin.metrics.asScala.toMap

  /**
    * @param ec
    * @return the consumer group stats from Kafka
    */
  def consumerGroupsStats(implicit ec: ExecutionContext): Future[Seq[ConsumerGroupStats]] = {
    val groups: CancelableFuture[Iterable[ConsumerGroupListing]] = consumerGroups
    val all: CancelableFuture[Future[Seq[ConsumerGroupStats]]] = groups.map { listings =>
      val futures: Iterable[CancelableFuture[ConsumerGroupStats]] = listings.map { cgl =>
        val groupId = cgl.groupId()
        consumerGroupsPositions(groupId).map { stats =>
          ConsumerGroupStats(groupId, stats)
        }
      }
      val list: Seq[Future[ConsumerGroupStats]] = futures.toSeq
      Future.sequence(list)
    }
    all.flatten
  }
  def consumerGroups(implicit ec: ExecutionContext): CancelableFuture[Seq[ConsumerGroupListing]] = {
    val result  = admin.listConsumerGroups()
    val kfuture = result.all()
    val future: Future[Seq[ConsumerGroupListing]] = Future {
      kfuture.get().asScala.toSeq
    }
    val cancel = Cancelable.apply(() => kfuture.cancel(true))
    CancelableFuture(future, cancel)
  }

  def consumerGroupsPositions(groupId: String)(implicit ec: ExecutionContext): CancelableFuture[Map[TopicPartition, OffsetAndMetadata]] = {
    val kfuture = admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata()
    val future  = Future(kfuture.get().asScala.toMap)
    val cancel  = Cancelable.apply(() => kfuture.cancel(true))
    CancelableFuture(future, cancel)
  }

  def topics(options: ListTopicsOptions = new ListTopicsOptions)(implicit ec: ExecutionContext): Future[Map[String, TopicListing]] = {
    val kFuture: KafkaFuture[util.Map[String, TopicListing]] = admin.listTopics(options).namesToListings()
    Future(kFuture.get().asScala.toMap)
  }

  def isClosed() = closed

  override def close(): Unit = {
    if (!closed) {
      logger.warn("Closing the admin client")
      closed = true
      admin.close()
    }
    logger.warn("Closed the admin client")
  }
}

object RichKafkaAdmin extends StrictLogging {

  /**
    * Make a blocking call to create the topic using the 'kafka4m.admin.topic' and kafka4m.admin client.
    *
    * This is very naughty to be done as a blocking call. I'm sorry.
    *
    * @param config the root configuration
    * @param ec
    * @return None if kafka4m.whenMissingTopic.create is true or the topic already exists, Some(topic) if the topic was created
    */
  def ensureTopicBlocking(config: Config)(implicit ec: ExecutionContext): Option[String] = {
    Using(RichKafkaAdmin(config)) { admin =>
      val whenMissingConfig = config.getConfig("kafka4m.whenMissingTopic")
      val topic             = kafka4m.adminTopic(config)

      if (whenMissingConfig.getBoolean("create")) {
        val numPartitions     = whenMissingConfig.getInt("numPartitions")
        val replicationFactor = whenMissingConfig.getInt("replicationFactor").toShort
        val timeout           = whenMissingConfig.getDuration("timeout", TimeUnit.MILLISECONDS)
        import concurrent.duration._

        val future = admin.getOrCreateTopic(topic, numPartitions, replicationFactor, timeout.millis)
        logger.info(s"checking topic '${topic}'")
        Await.result(future, timeout.millis) match {
          case None =>
            logger.info(s"Topic '${topic}' already exists")
            None
          case some =>
            logger.info(s"Created topic '${topic}' w/ $numPartitions partitions and replication factor $replicationFactor")
            some
        }
      } else {
        logger.info(s"kafka4m.whenMissingTopic.create is false - not checking topic '${topic}'")
        None
      }
    }
  }

  def apply(rootConfig: Config): RichKafkaAdmin = {
    val props: Properties  = Props.propertiesForConfig(rootConfig.getConfig("kafka4m.admin"))
    val admin: AdminClient = AdminClient.create(props)
    new RichKafkaAdmin(admin)
  }
}
