package pipelines.eval

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Observable, Observer, Pipe}
import pipelines.DynamicAvroRecord
import pipelines.core.{Rate, StreamStrategy}
import pipelines.eval.EvalReactive.throttle
import pipelines.kafka._

/**
  * This class represents a means to observe a continuous stream of data based on some data source which may change over time (e.g. some criteria is updated, and the output data is reflected)
  *
  * @param sourceForQuery
  * @param queryFeed
  * @param queryUpdates
  * @tparam A
  */
class EvalReactive[A] private (sourceForQuery: QueryRequest => Provider[Observable[A]], queryFeed: Observer[QueryRequest], queryUpdates: Observable[QueryRequest])
    extends StrictLogging {

  /**
    *
    * @param request the message to push to this feed
    */
  def update(request: StreamingFeedRequest): Unit = {
    logger.info(s" **> CLIENT SENT  $request")
    request match {
      case CancelFeedRequest           => queryFeed.onComplete()
      case Heartbeat                   =>
      case UpdateFeedRequest(newQuery) => queryFeed.onNext(newQuery)
    }
  }

  /** @return an observable of query requests and their values
    */
  def source: Observable[(QueryRequest, A)] = {
    queryUpdates.switchMap { nextQuery =>
      logger.info(s"Updated Query : $nextQuery")
      Observable(sourceForQuery(nextQuery)).bracket { source =>
        val dataSource = source.data
        // guard against this obs completing
        val data = dataSource.map(nextQuery -> _)

        logger.debug(s"Throttling ${nextQuery.messageLimit}}, ${nextQuery.streamStrategy}")
        throttle(data, nextQuery.messageLimit, nextQuery.streamStrategy)
      }(c => Task.evalOnce(c.close()))
    }
  }
}

/**
  * Provides a functions for ensuring observables are throttled/sampled according to a 'messageLimitPerSecond' and whether
  * we those messages should be dropped to ensure the latest values are sent
  */
object EvalReactive extends StrictLogging {

  type TopicName    = String
  type ReaderLookup = TopicName => Option[AvroReader[DynamicAvroRecord]]

  def apply[A](clientForQuery: QueryRequest => Provider[Observable[A]])(implicit scheduler: Scheduler): EvalReactive[A] = {
    val (queryIn: Observer[QueryRequest], queryOut: Observable[QueryRequest]) = Pipe.behavior((null: QueryRequest)).multicast
    new EvalReactive[A](clientForQuery, queryIn, queryOut.filter(_ != null))
  }

  def throttle[A](input: Observable[A], messageLimit: Option[Rate], strategy: StreamStrategy): Observable[A] = {

    (messageLimit, strategy) match {
      case (Some(Rate(limit, per)), StreamStrategy.Latest) =>
        input.bufferTimed(per).whileBusyDropEvents.map(select(_, limit)).switchMap(Observable.fromIterable)
      case (Some(Rate(limit, per)), StreamStrategy.All) =>
        input.bufferTimed(per).map(select(_, limit)).switchMap(Observable.fromIterable)
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
      val result = if (skip == 0) {
        data
      } else {
        val iter = data.iterator.zipWithIndex.collect {
          case (value, i) if i % skip == 0 => value
        }
        iter.toSeq.take(max)
      }

      if (result.nonEmpty) {
        logger.info(s"select(input=${size} elms, max=$max) returned ${result.size} (skip=$skip)")
      } else {
        logger.debug(s"select(input=${size} elms, max=$max) returned ${result.size} (skip=$skip)")
      }

      result
    }
  }

}
