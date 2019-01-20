package kafka4m.admin

import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import kafka4m.util.Props
import monix.execution.Scheduler
import org.apache.kafka.clients.admin._
import org.apache.kafka.common.KafkaFuture

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * A wrapper onto the admin API
  *
  * @param admin the underlying kafka client
  */
final class RichKafkaAdmin(val admin: AdminClient) extends AnyVal {

  def createTopicSync(name: String, timeout: FiniteDuration)(implicit ec: ExecutionContext): Unit = {
    val fut: KafkaFuture[Void] = createTopic(name).values().get(name)
    fut.get(timeout.toMillis, TimeUnit.MILLISECONDS)
  }

  def createTopic(name: String, numPartitions: Int = 1, replicationFactor: Short = 1)(implicit ec: ExecutionContext): CreateTopicsResult = {
    createTopic(new NewTopic(name, numPartitions, replicationFactor))
  }

  def createTopic(topic: NewTopic): CreateTopicsResult = {
    admin.createTopics(java.util.Collections.singletonList(topic))
  }

  def topics(options: ListTopicsOptions = new ListTopicsOptions)(implicit ec: ExecutionContext): Future[Map[String, TopicListing]] = {
    val kFuture: KafkaFuture[util.Map[String, TopicListing]] = admin.listTopics(options).namesToListings()
    import scala.collection.JavaConverters._
    Future(kFuture.get().asScala.toMap)
  }
}

object RichKafkaAdmin {
  def apply(rootConfig: Config)(implicit scheduler: Scheduler): RichKafkaAdmin = {
    val props: Properties  = Props.propertiesForConfig(rootConfig.getConfig("kafka4m.admin"))
    val admin: AdminClient = AdminClient.create(props)
    new RichKafkaAdmin(admin)
  }
}
