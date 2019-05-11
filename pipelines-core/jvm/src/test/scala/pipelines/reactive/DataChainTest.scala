package pipelines.reactive

import java.util.concurrent.atomic.AtomicInteger

import monix.reactive.Observable
import pipelines.data.{BaseCoreTest, WithScheduler}

class DataChainTest extends BaseCoreTest {
  "DataChain" should {
    "return the data source for the particular part id" in {



    }

//    "map some ints to data -> json -> string -> byte array, add as statistics tuple, then transform the statistics tuple back to json" in {
//      import TransformTest._
//      val input                 = 0 to 100
//      val raw                   = Observable.fromIterable(input)
//      val asData: Transform     = Transform.map(TransformTest.TestData.apply)
//      val asJson: Transform     = Transform.jsonEncoder[TestData]
//      val jsonStr: Transform    = Transform.jsonToString
//      val strToBytes: Transform = Transform.stringToUtf8
//
//      val counter                  = new AtomicInteger(0)
//      val Some(chained: Transform) = ??? // Repository.chain(Seq(asData, asJson, jsonStr, Transform.any { obs =>
////        obs.map { x =>
////          counter.incrementAndGet()
////          x
////        }
////      }, strToBytes))
//
//      val Some(output) = chained.applyTo(Data(raw))
//      val byteArrays: List[_] = WithScheduler { implicit s =>
//        output.asObservable.toListL.runSyncUnsafe(testTimeout)
//      }
//
//      byteArrays.size shouldBe input.size
//      counter.get shouldBe input.size
//      byteArrays.forall(_.isInstanceOf[Array[Byte]]) shouldBe true
//    }

  }
}
