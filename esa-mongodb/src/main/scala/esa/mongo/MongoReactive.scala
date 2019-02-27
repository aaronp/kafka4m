package esa.mongo

object MongoReactive {

  class ReactivePublisherForObservable[A](val obs :MongoObservable[A]) extends RPublisher[A] {
    override def subscribe(s: RSubscriber[_ >: A]): Unit = {
      obs.subscribe(new ObserverForSubscriber[A](s))
    }
  }

  class ObserverForSubscriber[A](val subscriber :RSubscriber[_ >: A]) extends MongoObserver[A] {
    override def onNext(result: A): Unit = {
      subscriber.onNext(result)
    }
    override def onError(e: Throwable): Unit = {
      subscriber.onError(e)
    }
    override def onComplete(): Unit                = {
      subscriber.onComplete()
    }
    override def onSubscribe(subscription: MongoSubscription): Unit = {
      val s = new SubscriptionForMongoSubscription(subscription)
      subscriber.onSubscribe(s)
    }
  }

  class SubscriptionForMongoSubscription(subscription: MongoSubscription) extends RSubscription {
    override def request(n: Long): Unit = subscription.request(n)
    override def cancel(): Unit         = subscription.unsubscribe()
  }

}
