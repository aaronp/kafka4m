package esa.mongo
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.StrictLogging
import monix.execution.CancelableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Completed, MongoCollection, Observer, SingleObservable}

class MongoConnectTest extends BaseMongoSpec with StrictLogging {

  "MongoConnect" should {
    "connect" in {

      val someC: MongoCollection[Document] = mongoDb.getCollection("users")

      val document: Document               = Document("x" -> 1)
      val res: SingleObservable[Completed] = someC.insertOne(document)
      val completed                        = new AtomicBoolean(false)

      import implicits._
      val head: CancelableFuture[Option[Completed]] = res.monix.runAsyncGetFirst

      res.subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = logger.info(s"onNext: $result")
        override def onError(e: Throwable): Unit     = logger.info(s"onError: $e")
        override def onComplete(): Unit = {
          logger.info("onComplete")
          completed.set(true)
        }
      })

      eventually {
        completed.get shouldBe true
      }
    }
  }
}
