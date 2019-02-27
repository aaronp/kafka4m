package esa
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

package object mongo {

  lazy val ioScheduler: SchedulerService = Scheduler.io("mongo-io")

  type MongoObservable[A] = org.mongodb.scala.Observable[A]
  type MongoObserver[A] = org.mongodb.scala.Observer[A]
  type MongoSubscription = org.mongodb.scala.Subscription

  type RPublisher[A] = org.reactivestreams.Publisher[A]
  type RSubscriber[A] = org.reactivestreams.Subscriber[A]
  type RSubscription = org.reactivestreams.Subscription
}
