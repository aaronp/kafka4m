package pipelines.client

import endpoints.xhr
import pipelines.kafka.{KafkaEndpoints, KafkaSupportEndpoints}

object KafkaQueryXhrClient extends xhr.future.Endpoints with xhr.circe.JsonSchemaEntities with KafkaEndpoints with KafkaSupportEndpoints
