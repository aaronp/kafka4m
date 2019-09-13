package kafka4m
package streams

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import kafka4m.util.Props
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import org.apache.kafka.streams.kstream.ForeachAction
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, Topology}

/** A StreamConsumer represents the pieces used to create a kafka streams consumer.
  *
  * This basically provides some hooks to access the pieces used to assemble the data feed coming from kafka (via the streams builder) into our input.
  *
  * @param streamsConfig the config used to bootstrap our kafka (streams) consumer,
  * @param input         the input of our stream, driving by the 'foreach' kafka Streams consumer
  */
final class StreamConsumer(streamsConfig: Config, input: Observer[KeyValue]) extends AutoCloseable with StrictLogging {
  private val builder = new StreamsBuilder
  private val appId   = streamsConfig.getString("application.id")
  private val topic   = streamsConfig.getString("topic")
  logger.info(s"Streaming from $topic")
  private val props = Props.propertiesForConfig(streamsConfig)

  @volatile private var closed = false

  // kafka streams API adapter
  private object action extends ForeachAction[Key, Bytes] {
    override def apply(key: Key, valueBytes: Bytes): Unit = {
      logger.trace(s"KafkaStreams.foreach: $key : ${valueBytes.length}")
      if (!closed) {
        input.onNext(key -> valueBytes)
      }
    }
  }

  builder.stream[Key, Bytes](topic).foreach(action)
  val topology: Topology    = builder.build()
  val streams: KafkaStreams = new KafkaStreams(topology, props)
  streams.start()

  override def close(): Unit = {
    closed = true
    input.onComplete()
    streams.close()
  }
}

object StreamConsumer {

  /** A handle which wraps the consumer together with its output
    *
    * @param consumer  an adapter for pushing data into the output from the kafka streams
    * @param output    the key/value data coming out
    * @param scheduler an io scheduler to drive the Kafka IO
    */
  case class Setup(consumer: StreamConsumer, output: Observable[KeyValue], scheduler: Scheduler) extends AutoCloseable {
    override def close(): Unit = {
      consumer.close()
    }
  }

  def apply(rootConfig: Config)(implicit scheduler: Scheduler): Setup = {
    val (input, output: Observable[(String, Array[Byte])]) = Pipe.publishToOne[KeyValue].unicast

    val consumer: StreamConsumer = apply(rootConfig, input)
    new Setup(consumer, output, scheduler)
  }

  def apply(rootConfig: Config, input: Observer[KeyValue]): StreamConsumer = {
    val config = rootConfig.getConfig("kafka4m.streams")

    import args4c.implicits._
    val id = Props.replaceUniqueId(config.getString("application.id"))
    new StreamConsumer(config.set("application.id", id), input)
  }

}
