package esa.mongo
import org.mongodb.scala.{Document, MongoCollection}

/**
  * Just verify the mongo env works as we expect it to
  */
class MongoEnvTest extends BaseMongoSpec {

  "start/stop Mongo" should {
    "work" in {
      if (isMongoRunning) {
        stopMongo() shouldBe true
      }
      isMongoRunning() shouldBe false
      startMongo() shouldBe true
      isMongoRunning() shouldBe true
    }
    "keep the data persistent between runs" in {
      ensureMongoIsRunning()
      val test: MongoCollection[Document] = mongoDb.getCollection("test")
    }
  }
}
