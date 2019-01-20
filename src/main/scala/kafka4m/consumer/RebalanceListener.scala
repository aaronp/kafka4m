package kafka4m.consumer

import java.util

import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

case object RebalanceListener extends ConsumerRebalanceListener with StrictLogging {

  override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
    logger.info(partitions.asScala.mkString("onPartitionsRevoked(", ", ", ")"))
  }

  override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
    logger.info(partitions.asScala.mkString("onPartitionsAssigned(", ", ", ")"))
  }
}
