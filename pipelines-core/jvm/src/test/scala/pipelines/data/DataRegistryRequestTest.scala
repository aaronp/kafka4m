package pipelines.data

import io.circe.Json
import pipelines.core.{Rate, StreamStrategy}

class DataRegistryRequestTest extends BaseCoreTest {

  "DataRegistryRequest" should {

    import ModifyObservableRequest._
    val all = List[DataRegistryRequest](
      Connect("sourceKey", "sinkKey"),
      ModifySourceRequest("sourceKey", "newKey", RateLimit(Rate.perSecond(12), StreamStrategy.All)),
      ModifySourceRequest("sourceKey", "newKey", Filter("expression")),
      ModifySourceRequest("sourceKey", "newKey", AddStatistics(true)),
      ModifySourceRequest("sourceKey", "newKey", Persist("/data")),
      UpdateSourceRequest("sourceKey", Take(2)),
      UpdateSourceRequest("sourceKey", Generic("foo", Json.fromString("hi")))
      //      ModifySourceRequest("sourceKey", "newKey", ModifyObservableRequest.MapType((_:Int).toString),
    )

    all.foreach { expected =>
      s"serialize $expected to/from json" in {
        import io.circe.parser._
        import io.circe.syntax._
        decode[DataRegistryRequest](expected.asJson.noSpaces) shouldBe Right(expected)
      }
    }

  }
}
