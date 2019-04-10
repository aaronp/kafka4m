package kafkaquery.client

import endpoints.xhr
import kafkaquery.kafka.{KafkaEndpoints, KafkaSupportEndpoints}

object KafkaQueryXhrClient extends xhr.future.Endpoints with xhr.circe.JsonSchemaEntities with KafkaEndpoints with KafkaSupportEndpoints
