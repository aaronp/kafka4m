package kafkaquery.client

import endpoints.xhr
import kafkaquery.kafka.KafkaEndpoints

object KafkaQueryXhrClient extends xhr.future.Endpoints with xhr.circe.JsonSchemaEntities with KafkaEndpoints
