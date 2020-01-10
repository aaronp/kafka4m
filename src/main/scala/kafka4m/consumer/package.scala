package kafka4m

import monix.reactive.Observable

package object consumer {

  private[consumer] def repeatedObservable[A](producer: => Iterable[A]): Observable[A] = {
    val iterators: Observable[Iterable[A]] = Observable.repeatEval(producer)
    iterators.flatMap { iter: Iterable[A] =>
      Observable.fromIterable(iter)
    }
  }
}
