package kafka4m.consumer

import java.util

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition

object DelegateListener {
  def apply(first: ConsumerRebalanceListener, second: ConsumerRebalanceListener, theRest: ConsumerRebalanceListener*): DelegateListener = {
    DelegateListener(first +: second +: theRest)
  }
}

final case class DelegateListener(delegates: Seq[ConsumerRebalanceListener]) extends ConsumerRebalanceListener {
  override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
    delegates.foreach(_.onPartitionsRevoked(partitions))
  }

  override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
    delegates.foreach(_.onPartitionsAssigned(partitions))
  }
}
