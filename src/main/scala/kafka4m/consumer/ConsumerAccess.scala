package kafka4m.consumer

import scala.concurrent.Future

/**
  * Access to Kafka is enforced to be single-threaded.
  *
  * 'ConsumerAccess' will expose access to our [[RichKafkaConsumer]] on the thread on which it was created.
  *
  * Have fun, but Be Careful!
  */
trait ConsumerAccess {
  type Key
  type Value
  def withConsumer[A](thunk: RichKafkaConsumer[Key, Value] => A): Future[A]
}
