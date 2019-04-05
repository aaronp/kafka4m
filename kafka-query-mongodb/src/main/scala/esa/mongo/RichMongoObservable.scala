package esa.mongo
import monix.reactive.Observable

class RichMongoObservable[A](obs: MongoObservable[A]) {

  def asPublisher: RPublisher[A] = {
    new MongoReactive.ReactivePublisherForObservable(obs)
  }

  def monix: Observable[A] = Observable.fromReactivePublisher(asPublisher)
}
