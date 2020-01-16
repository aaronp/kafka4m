package kafka4m

import monix.eval.Task
import monix.reactive.Observable

package object consumer {

  private[consumer] def repeatedObservable[A](producer: => Iterable[A]): Observable[A] = {
    val iterators: Observable[Iterable[A]] = Observable.repeatEvalF(Task.eval(producer))
    iterators.flatMap { iter: Iterable[A] =>
      Observable.fromIterable(iter)
    }
  }
}
