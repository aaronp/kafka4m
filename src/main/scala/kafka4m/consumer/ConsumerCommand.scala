package kafka4m.consumer
import scala.concurrent.Promise
import scala.util.Try

/**
  * A wrapper for an operation which needs to be performed on the kafka consumer thread
  *
  * @param f the operation to perform on a KafkaConsumer
  * @param promise the result promise to complete once this task completes
  * @tparam K the kafka consumer key type
  * @tparam V the kafka consumer value type
  * @tparam A the task result type
  */
private[consumer] final case class ExecOnConsumer[K, V, A](f: RichKafkaConsumer[K, V] => A, promise: Promise[A] = Promise[A]()) {
  def run(inst: RichKafkaConsumer[K, V]) = {
    promise.tryComplete(Try(f(inst)))
  }
}
