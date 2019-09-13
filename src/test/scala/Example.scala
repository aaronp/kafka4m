import com.typesafe.config.ConfigFactory
import monix.eval.Task
import monix.reactive.{Consumer, Observable}
import org.apache.kafka.clients.consumer.ConsumerRecord

class Example {

  val config = ConfigFactory.load()

  // write data to kafka (assumes a configuration akin to kafka4m.producer.topic = someNewTopic)
  val kafkaWriter: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)

  // read data from kafka (assumes a configuration akin to kafka4m.consumer.topic = originalTopic)
  val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.read(config)

  // then we'd write it back into kafka like this.
  val task: Task[Long] = kafkaData.map(r => (r.key, r.value)).consumeWith(kafkaWriter)

}
