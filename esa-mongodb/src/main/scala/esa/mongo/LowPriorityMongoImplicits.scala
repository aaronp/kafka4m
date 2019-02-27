package esa.mongo
import esa.mongo.MongoReactive.ReactivePublisherForObservable
import monix.execution.Scheduler
import monix.reactive.Observable

trait LowPriorityMongoImplicits {

  implicit def scheduler: Scheduler = ioScheduler

  implicit def mongoObsAsReactiveSubscriber[A](obs :MongoObservable[A]) = new {
    def asPublisher: RPublisher[A] = {
      new ReactivePublisherForObservable(obs)
    }
    def asMonix = {
      Observable.fromReactivePublisher(asPublisher)
    }
  }
}
