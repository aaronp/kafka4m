package esa.mongo
import java.util.concurrent.atomic.AtomicBoolean

import monix.execution.CancelableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Completed, Observer, SingleObservable}

class MongoConnectTest extends BaseMongoSpec {

  "MongoConnect" should {
    "connect" in {

      val c     = MongoConnect("serviceUser", "changeTh1sDefaultPasswrd".toCharArray, "esa", "mongodb://localhost:9010")
      val someC = MongoConnect.collection(c)

//      val document: Document               = Document("_id" -> 1, "x" -> 1)
      val document: Document               = Document("x" -> 1)
      val res: SingleObservable[Completed] = someC.insertOne(document)
      val completed                        = new AtomicBoolean(false)

      import implicits._
      val head: CancelableFuture[Option[Completed]] = res.asMonix.runAsyncGetFirst

      res.subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println(s"onNext: $result")
        override def onError(e: Throwable): Unit     = println(s"onError: $e")
        override def onComplete(): Unit = {
          println("onComplete")
          completed.set(true)
        }
      })

      eventually {
        completed.get shouldBe true
      }
    }
  }
}
