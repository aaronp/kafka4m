package kafka4m.consumer

import monix.reactive.Observable

/**
  * A means of ordering 'A' assuming an incrementing long value.
  *
  * This was created to ensure we can piece together a specific contiguous ordering after asynchronously dispatching a zipped observable to different tasks
  *
  * @tparam A
  */
trait ContiguousOrdering[A] {

  /** @param value the value from which we should derive a long value
    * @return the long index for this value
    */
  def longValue(value: A): Long
}

object ContiguousOrdering {

  def apply[A](implicit instance: ContiguousOrdering[A]): ContiguousOrdering[A] = instance

  def sort[A: ContiguousOrdering](data: Observable[A]): Observable[A] = {
    val tuples: Observable[(Buffer[A], Seq[A])] = data.scan((Buffer[A](), Seq[A]())) {
      case ((state, _), next) => state.offer(next)
    }
    tuples.map(_._2).flatMap(Observable.fromIterable)
  }

  private case class Buffer[A: ContiguousOrdering](buffer: Seq[A] = Seq(), lastEmitted: Option[Long] = None) {
    private def pop(): (Buffer[A], Seq[A]) = {
      buffer match {
        case Seq()        => (this, Seq())
        case head +: tail => copy(buffer = tail).offer(head)
      }
    }

    private def bufferItem(a: A): Seq[A] = {
      val value        = ContiguousOrdering[A].longValue(a)
      val (head, tail) = buffer.span(x => ContiguousOrdering[A].longValue(x) <= value)
      val newBuffer    = head ++ (a +: tail)

      val orderedBuffer = newBuffer.map(ContiguousOrdering[A].longValue)
      newBuffer
    }

    def offer(a: A): (Buffer[A], Seq[A]) = {
      val nextValue = ContiguousOrdering[A].longValue(a)

      lastEmitted match {
        case None => (copy(lastEmitted = Option(nextValue)), Seq(a))
        case Some(prevValue) if nextValue == prevValue + 1 =>
          val (newState, emission) = copy(lastEmitted = Option(nextValue)).pop()
          (newState, a +: emission)
        case Some(prevValue) if nextValue <= prevValue => (this, Seq())
        case Some(_) =>
          (copy(buffer = bufferItem(a)), Seq())
      }
    }
  }

  implicit object ForLong extends ContiguousOrdering[Long] {
    override def longValue(value: Long): Long = value
  }

  implicit object ForInt extends ContiguousOrdering[Int] {
    override def longValue(value: Int) = value.toLong
  }
  implicit def forTuple2[A]: ContiguousOrdering[(Long, A)] = new ContiguousOrdering[(Long, A)] {
    override def longValue(value: (Long, A)): Long = value._1
  }
  implicit def forTuple3[A, B]: ContiguousOrdering[(Long, A, B)] = new ContiguousOrdering[(Long, A, B)] {
    override def longValue(value: (Long, A, B)): Long = value._1
  }
}
