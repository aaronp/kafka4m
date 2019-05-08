package pipelines.data

import pipelines.core.{ByteArray, AvroRecord, JsonRecord, ProtobufRecord}

class DataRegistryResponseTest extends BaseEvalTest {
  "DataRegistryResponse" should {

    List[DataRegistryResponse](
      SourceNotFoundResponse("sourceKey"),
      SinkNotFoundResponse("sinkKey"),
      SourceSinkMismatchResponse("sourceKey", "sinkKey", ByteArray, JsonRecord),
      SourceAlreadyExistsResponse("sourceKey"),
      SinkAlreadyExistsResponse("sinkKey"),
      SourceCreatedResponse("sourceKey", JsonRecord),
      SinkCreatedResponse("sinkKey", JsonRecord),
      SourceUpdatedResponse("sourceKey", "updated"),
      UnsupportedTypeMappingResponse("sourceKey", ProtobufRecord, AvroRecord),
      ConnectResponse("sourceKey", "sinkKey"),
      SourceErrorResponse("sourceKey", "bang")
    ).foreach { expected =>
      s"serialize $expected to/from json" in {
        import io.circe.parser._
        import io.circe.syntax._
        decode[DataRegistryResponse](expected.asJson.noSpaces) shouldBe Right(expected)
      }
    }

  }
}
