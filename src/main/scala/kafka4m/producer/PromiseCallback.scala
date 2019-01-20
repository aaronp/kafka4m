package kafka4m.producer

import org.apache.kafka.clients.producer.{Callback, RecordMetadata}

import scala.concurrent.Promise

final case class PromiseCallback(promise: Promise[RecordMetadata] = Promise[RecordMetadata]()) extends Callback {
  override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
    if (exception != null) {
      promise.tryFailure(exception)
    } else {
      promise.trySuccess(metadata)
    }
  }

  def future: concurrent.Future[RecordMetadata] = promise.future
}
