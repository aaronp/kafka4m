## Usage

The kafka configurations are based on the the kafka4m typesafe configuration. In practice the intent is to achieve similar things
to what Kafka Streams gives you (but w/o relying on the kafka streams API)

## Copy A Kafka Topic:  
```$xslt
     
// write data to kafka (assumes a configuration akin to kafka4m.producer.topic = someNewTopic)
val kafkaWriter: Consumer[Array[Byte], Long] = kafka4m.writeBytes(config)

// read data from kafka (assumes a configuration akin to kafka4m.consumer.topic = originalTopic)
val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.read(config)

// here's our app:
val task: Task[Long] = kafkaData.map(_.value).consumeWith(kafkaWriter)
``` 

