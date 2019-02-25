package esa.mongo
import java.util.concurrent.atomic.AtomicBoolean

import org.mongodb.scala.{Completed, Observer, SingleObservable}
import org.mongodb.scala.bson.collection.immutable.Document
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSuite, Matchers, WordSpec}

class MongoConnectTest extends WordSpec with Matchers with Eventually {

  "MongoConnect" should {
    "connect" in {
      val c = MongoConnect.clientForUri("mongodb://localhost:32768")
      val someC = MongoConnect.collection(c)

      val document: Document = Document("_id" -> 1, "x" -> 1)
      val res: SingleObservable[Completed] = someC.insertOne(document)
      val completed = new AtomicBoolean(false)
      res.subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println(s"onNext: $result")
        override def onError(e: Throwable): Unit = println(s"onError: $e")
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
