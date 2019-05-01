package pipelines.data

class DataRegistryRequestTest extends BaseEvalTest {

  "DataRegistryRequest" should {

    List[DataRegistryRequest](
      Connect("sourceKey", "sinkKey"),
      UpdateFilterRequest("sourceKey", "filterExpression"),
      FilterSourceRequest("sourceKey", "newSourceKey", "filterExpression"),
      ChangeTypeRequest("sourceKey", "newSourceKey", ProtobufRecord)
    ).foreach { expected =>
      s"serialize $expected to/from json" in {
        import io.circe.parser._
        import io.circe.syntax._
        decode[DataRegistryRequest](expected.asJson.noSpaces) shouldBe Right(expected)
      }
    }

  }
}
