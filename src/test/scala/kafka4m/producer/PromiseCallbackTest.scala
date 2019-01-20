package kafka4m.producer

import kafka4m.BaseKafka4mSpec

class PromiseCallbackTest extends BaseKafka4mSpec {

  "PromiseCallback.onCompletion" should {
    "complete the future with an exception if set" in {
      val underTest = PromiseCallback()
      underTest.onCompletion(null, new Exception("bang"))
      val exp = intercept[Exception] {
        underTest.future.futureValue
      }
      exp.getMessage should include("bang")
    }
  }
}
