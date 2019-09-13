package kafka4m

import args4c.ConfigApp
import com.typesafe.config.Config
import monix.eval.Task
import monix.execution.CancelableFuture
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.util.Try

object Kafka4mApp extends ConfigApp {
  override type Result = Unit

  override def run(config: Config) = {
    val action = Try(config.getString("kafka4m.action")).getOrElse("")
    action match {
      case "read" =>
        import org.apache.kafka.clients.consumer.ConsumerRecord
        val kafkaData: Observable[ConsumerRecord[Key, Bytes]] = kafka4m.read(config)

      case "write" =>
        val kafkaWriter: Consumer[Array[Byte], Long] = kafka4m.writeBytes(config)
        val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.read(config)
        val task: Task[Long] = kafkaData.map(_.value).consumeWith(kafkaWriter)

        val numberWritten: CancelableFuture[Long] = kafka4m.util.Schedulers.using { s =>
          task.runToFuture(s)
        }
      case "" =>
    }
  }
}
