package kafkaquery.eval

import com.typesafe.scalalogging.StrictLogging
import kafkaquery.eval.KafkaReactive.{CreateFilter, throttle}
import kafkaquery.kafka.{CancelFeedRequest, Heartbeat, QueryRequest, Rate, StreamStrategy, StreamingFeedRequest, UpdateFeedRequest}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import org.reactivestreams.Publisher

/**
  * This class represents a means to observe a continuous stream of data based on some data source which may change over time (e.g. some criteria is updated, and the output data is reflected)
  *
  * @param sourceForQuery
  * @param queryFeed
  * @param queryUpdates
  * @tparam A
  */
class KafkaReactive[A] private (sourceForQuery: QueryRequest => KafkaReactive.Source[A],
                                newFilter: CreateFilter[A],
                                queryFeed: Observer[QueryRequest],
                                queryUpdates: Observable[QueryRequest])
    extends StrictLogging {

  def updateQuery(initialQuery: QueryRequest) = {
    queryFeed.onNext(initialQuery)
  }

  def update(request: StreamingFeedRequest) = {
    request match {
      case CancelFeedRequest           => queryFeed.onComplete()
      case Heartbeat                   =>
      case UpdateFeedRequest(newQuery) => updateQuery(newQuery)
    }
  }

  def source: Observable[A] = {
    val bracketed = queryUpdates
      .map(q => q -> sourceForQuery(q))
      .bracket {
        case (query, client) =>
          // guard against this obs completing
          val infinite: Observable[A] = {
            val data: Observable[A] = Observable
              .fromIterator(Task(client.iterator.map { next =>
                logger.info(s"ITERATOR.NEXT: $next")
                next
              }))
              .dump("D8A")
            val dontStop = Observable.never[A]
            data ++ dontStop
          }

          val filter = newFilter(query.filterExpression)

          throttle(infinite.filter(filter), query.messageLimit, query.streamStrategy)
      } { tuple =>
        Task.evalOnce(tuple._2.close())
      }

    bracketed
  }
}

/**
  * Provides a functions for ensuring observables are throttled/sampled according to a 'messageLimitPerSecond' and whether
  * we those messages should be dropped to ensure the latest values are sent
  */
object KafkaReactive {
  type Source[A]       = Iterable[A] with AutoCloseable
  type Expression      = String
  type Filter[A]       = A => Boolean
  type CreateFilter[A] = Expression => Filter[A]

  def apply[A](newFilter: CreateFilter[A], clientForQuery: QueryRequest => Source[A])(implicit scheduler: Scheduler): KafkaReactive[A] = {
    val (queryIn: Observer[QueryRequest], queryOut: Observable[QueryRequest]) = Pipe.behavior((null: QueryRequest)).multicast
    new KafkaReactive[A](clientForQuery, newFilter, queryIn, queryOut.filter(_ != null).dump("onQueryUpdate"))
  }

  def throttle[A](input: Publisher[A], messageLimit: Option[Rate], strategy: StreamStrategy): Observable[A] = {
    val obs: Observable[A] = Observable.fromReactivePublisher(input)
    throttle(obs, messageLimit, strategy)
  }

  def throttle[A](input: Observable[A], messageLimit: Option[Rate], strategy: StreamStrategy): Observable[A] = {

    (messageLimit, strategy) match {
      case (Some(Rate(limit, per)), StreamStrategy.Latest) =>
        input.bufferTimed(per).dump("\tBUFF").whileBusyDropEvents.map(select(_, limit)).dump("\tSEL").flatMap(Observable.fromIterable)
      case (Some(Rate(limit, per)), StreamStrategy.All) =>
        input.bufferTimed(per).map(select(_, limit)).flatMap(Observable.fromIterable)
      case (None, StreamStrategy.Latest) =>
        input.whileBusyDropEvents
      case (None, StreamStrategy.All) =>
        input
    }
  }

  /** samples 'max' elements from the given sequence
    *
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

}
