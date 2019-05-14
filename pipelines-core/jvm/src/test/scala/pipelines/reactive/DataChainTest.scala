package pipelines.reactive

import pipelines.data.BaseCoreTest

import scala.util.{Success, Try}

class DataChainTest extends BaseCoreTest with RepoTestData {
  "DataChain.addTransform" should {
    "an updated fee" in withScheduler { implicit sched =>
      val double = Transform.map[Int, Int](_ * 2)
      val myRepo = repo.withTransform("double", double)

      val Right(chain)               = myRepo.createChain("ints")
      val Right((updatedChain, key)) = chain.addTransform(chain.sourceKey, "evens", myRepo.transformsByName("evens"))
      key shouldBe chain.maxKey + 1
      updatedChain.sourceKey shouldBe chain.sourceKey
      updatedChain.maxKey shouldBe key

      val result: Try[List[_]] = updatedChain.connect(updatedChain.maxKey)(_.toListL.runSyncUnsafe(testTimeout))
      result shouldBe Success((0 to 100).filter(isEven).toList)
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
