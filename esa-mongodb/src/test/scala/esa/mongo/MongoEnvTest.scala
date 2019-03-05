package esa.mongo
import java.util.UUID

import io.circe
import org.mongodb.scala.{Completed, Document, MongoCollection, MongoDatabase}
import org.scalatest.GivenWhenThen

/**
  * Just verify the mongo env works as we expect it to
  */
class MongoEnvTest extends BaseMongoSpec with GivenWhenThen {

  import MongoEnvTest._

  "start/stop Mongo" should {
    "work" in {
      if (isMongoRunning) {
        stopMongo() shouldBe true
      }
      isMongoRunning() shouldBe false
      startMongo() shouldBe true
      isMongoRunning() shouldBe true
    }

    "persist data between restarts" in {
      Given("A running database")
      ensureMongoIsRunning()

      And("A new collection")
      import LowPriorityMongoImplicits._
      import io.circe.generic.auto._

      val newCollectionName                                  = ("test" + UUID.randomUUID()).take(20)
      def test(db: MongoDatabase): MongoCollection[Document] = db.getCollection(newCollectionName)

      def readBack(db: MongoDatabase): List[Either[circe.Error, SomeValue]] = {
        val found: List[Document] = test(db).find().monix.toListL.runSyncUnsafe(testTimeout)
        found.map(_.as[SomeValue])
      }

      When("we insert some data (and prove we can read it back)")
      insertDocument(test(mongoDb), SomeValue(123))

      readBack(mongoDb) shouldBe List(Right(SomeValue(123)))

      And("restart the database")
      restartMongo() shouldBe true

      Then("we should still be able to read the same data back from our new collection")
      readBack(mongoDb) shouldBe List(Right(SomeValue(123)))
      mongoDb.drop().monix.runAsyncGetLast.futureValue shouldBe Some(Completed())
    }
    "write and read back data" in {
      ensureMongoIsRunning()
      import LowPriorityMongoImplicits._
      import io.circe.generic.auto._

      val newCollectionName                                  = ("test" + UUID.randomUUID()).take(20)
      def test(db: MongoDatabase): MongoCollection[Document] = db.getCollection(newCollectionName)

      def readBack(db: MongoDatabase): List[Either[circe.Error, SomeValue]] = {
        val found: List[Document] = test(db).find().monix.toListL.runSyncUnsafe(testTimeout)
        found.map(_.as[SomeValue])
      }

      insertDocument(test(mongoDb), SomeValue(123))
      readBack(mongoDb) shouldBe List(Right(SomeValue(123)))
    }
  }
}

object MongoEnvTest {
  case class SomeValue(x: Int)
}
