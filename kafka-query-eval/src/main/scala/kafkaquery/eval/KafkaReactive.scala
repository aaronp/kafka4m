package kafkaquery.eval

import kafkaquery.kafka.StreamStrategy
import monix.reactive.Observable
import org.reactivestreams.Publisher

import scala.concurrent.duration._

object KafkaReactive {

  def selectSimple[A](data: Seq[A], max: Int) = data.take(max)

  /**
    * @param data the data to sample
    * @param max the number of elements to keep
    * @tparam A
    * @return 'max' as a sample of the data
    */
  def select[A](data: Seq[A], max: Int): Seq[A] = {
    if (max <= 0) Nil
    else {
      val size = data.size
      val skip = size / max
      if (skip == 0) {
        data
      } else {
        val iter = data.iterator.zipWithIndex.collect {
          case (value, i) if i % skip == 0 => value
        }
        iter.toSeq.take(max)
      }
    }
  }

  def apply[A](input: Publisher[A], messageLimitPerSecond: Option[Int], strategy: StreamStrategy): Observable[A] = {
    val obs: Observable[A] = Observable.fromReactivePublisher(input)
    apply(obs, messageLimitPerSecond, strategy)
  }

  def apply[A](input: Observable[A], messageLimitPerSecond: Option[Int], strategy: StreamStrategy): Observable[A] = {
    (messageLimitPerSecond, strategy) match {
      case (Some(limit), StreamStrategy.Latest) =>
        input.bufferTimed(1.second).whileBusyDropEvents.map(select(_, limit)).flatMap(Observable.fromIterable)
      case (Some(limit), StreamStrategy.All) =>
        input.bufferTimed(1.second).map(select(_, limit)).flatMap(Observable.fromIterable)
      case (None, StreamStrategy.Latest) =>
        input.whileBusyDropEvents
      case (None, StreamStrategy.All) =>
        input
    }
  }

}
