package pipelines.data

import pipelines.core.{Enrichment, JsonRecord, Rate, StreamStrategy}

class DataRegistryRequestTest extends BaseEvalTest {

  "DataRegistryRequest" should {

    List[DataRegistryRequest](
      Connect("sourceKey", "sinkKey"),
      EnrichSourceRequest("sourceKey", "newKey", Enrichment.RateLimit(Rate.perSecond(12), StreamStrategy.All)),
      EnrichSourceRequest("sourceKey", "newKey", Enrichment.Filter("expression")),
      EnrichSourceRequest("sourceKey", "newKey", Enrichment.MapType(JsonRecord)),
      EnrichSourceRequest("sourceKey", "newKey", Enrichment.AddStatistics(true)),
      EnrichSourceRequest("sourceKey", "newKey", Enrichment.PersistData("/data")),
      UpdateEnrichedSourceRequest("sourceKey", Enrichment.PersistData("/data"))
    ).foreach { expected =>
      s"serialize $expected to/from json" in {
        import io.circe.parser._
        import io.circe.syntax._
        decode[DataRegistryRequest](expected.asJson.noSpaces) shouldBe Right(expected)
      }
    }

  }
}
