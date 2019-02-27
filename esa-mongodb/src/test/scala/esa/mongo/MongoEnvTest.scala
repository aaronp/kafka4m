package esa.mongo

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
  }
}
