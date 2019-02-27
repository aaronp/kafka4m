package esa.mongo
import monix.execution.Scheduler
import org.mongodb.scala.{Document, MongoCollection}

trait LowPriorityMongoImplicits {

  implicit def scheduler: Scheduler = ioScheduler

  implicit def mongoObsAsReactiveSubscriber[A](obs :MongoObservable[A]) = new RichMongoObservable[A](obs)
  implicit def asRichCollection(collection : MongoCollection[Document]) = new RichCollection(collection)
}
