package esa.mongo
import java.util.concurrent.atomic.AtomicBoolean

import monix.execution.CancelableFuture
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Completed, Observer, SingleObservable}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class MongoConnectTest extends WordSpec with Matchers with Eventually {

  def testTimeout: FiniteDuration = 5.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

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
