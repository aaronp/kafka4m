package kafka4m.consumer
import scala.concurrent.Promise
import scala.util.Try

private[consumer] final case class ExecOnConsumer[K, V, A](f: RichKafkaConsumer[K, V] => A, promise: Promise[A] = Promise[A]()) {
  def run(inst: RichKafkaConsumer[K, V]) = {
    promise.tryComplete(Try(f(inst)))
  }
}
