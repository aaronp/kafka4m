package kafka4m.consumer

import java.util

import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

final class RebalanceListener extends ConsumerRebalanceListener with StrictLogging {
  private var currentAssignments: Set[TopicPartition] = Set.empty

  def assignments: Set[TopicPartition] = currentAssignments

  private object Lock
  override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
    if (partitions != null) Lock.synchronized {
      currentAssignments = currentAssignments -- partitions.asScala.toSet
    }
    logger.info(partitions.asScala.mkString("onPartitionsRevoked(", ", ", s"), currentAssignments is now ${currentAssignments}"))
  }

  override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
    Lock.synchronized {
      currentAssignments = currentAssignments ++ partitions.asScala.toSet
    }
    logger.info(partitions.asScala.mkString("onPartitionsAssigned(", ", ", s"), currentAssignments is now ${currentAssignments}"))
  }
}
