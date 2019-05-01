package pipelines.data

class DataRegistryResponseTest extends BaseEvalTest {
  "DataRegistryResponse" should {

    List[DataRegistryResponse](
      SourceNotFoundResponse("sourceKey"),
      SinkNotFoundResponse("sinkKey"),
      SourceSinkMismatchResponse("sourceKey", "sinkKey", ByteArray, JsonRecord),
      SourceAlreadyExistsResponse("sourceKey"),
      SourceCreatedResponse("sourceKey", JsonRecord),
      UnsupportedTypeMappingResponse("sourceKey", SpecificAvroRecord, GenericAvroRecord),
      ConnectResponse("sourceKey", "sinkKey"),
      ErrorCreatingSource("sourceKey", "bang")
    ).foreach { expected =>
      s"serialize $expected to/from json" in {
        import io.circe.parser._
        import io.circe.syntax._
        decode[DataRegistryResponse](expected.asJson.noSpaces) shouldBe Right(expected)
      }
    }

  }
}
