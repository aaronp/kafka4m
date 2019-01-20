package kafka4m.producer

import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.{Callback, RecordMetadata}

object LoggingCallback extends Callback with StrictLogging {
  override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
    if (exception != null) {
      logger.error(s"onCompletion($metadata) : $exception", exception)
    } else {
      logger.debug(s"onCompletion($metadata)")
    }
  }
}
