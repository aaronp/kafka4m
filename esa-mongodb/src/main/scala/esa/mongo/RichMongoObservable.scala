package esa.mongo
import esa.mongo.MongoReactive.ReactivePublisherForObservable
import monix.reactive.Observable

class RichMongoObservable[A](obs: MongoObservable[A]) {

  def asPublisher: RPublisher[A] = {
    new ReactivePublisherForObservable(obs)
  }

  def monix: Observable[A] = Observable.fromReactivePublisher(asPublisher)
}
