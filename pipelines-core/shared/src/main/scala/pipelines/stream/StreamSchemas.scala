package pipelines.stream

import io.circe.Json
import pipelines.core.Enrichment
import pipelines.data.DataRegistryResponse

trait StreamSchemas extends endpoints.circe.JsonSchemas {

  implicit def ListSourceResponseSchema: JsonSchema[ListSourceResponse]     = JsonSchema(implicitly, implicitly)
  implicit def PeekResponseSchema: JsonSchema[PeekResponse]                 = JsonSchema(implicitly, implicitly)
  implicit def EnrichmentSchema: JsonSchema[Enrichment]                     = JsonSchema(implicitly, implicitly)
  implicit def DataRegistryResponseSchema: JsonSchema[DataRegistryResponse] = JsonSchema(implicitly, implicitly)
  implicit def CirceJsonSchema: JsonSchema[Json]                            = JsonSchema(implicitly, implicitly)

}
