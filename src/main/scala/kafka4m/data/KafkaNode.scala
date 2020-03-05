package kafka4m.data
import org.apache.kafka.common.Node

case class KafkaNode(id: Int, host: String, port: Int, rack: String)

object KafkaNode {
  def apply(node: Node): KafkaNode = {
    new KafkaNode(
      id = node.id,
      host = Option(node.host).getOrElse(""),
      port = node.port,
      rack = Option(node.rack).getOrElse("")
    )
  }
}
