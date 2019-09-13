## Usage

The kafka configurations are based on the the kafka4m typesafe configuration. In practice the intent is to achieve similar things
to what Kafka Streams gives you (but w/o relying on the kafka streams API)

## Copy A Kafka Topic:  
```$xslt
  val config = ConfigFactory.load()

  // write data to kafka (assumes a configuration akin to kafka4m.producer.topic = someNewTopic)
  val kafkaWriter: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)

  // read data from kafka (assumes a configuration akin to kafka4m.consumer.topic = originalTopic)
  val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.read(config)

  // then we'd write it back into kafka like this.
  val task: Task[Long] = kafkaData.map(r => (r.key, r.value)).consumeWith(kafkaWriter)
``` 

That provides the base primitives -- getting data into and out of Kafka.

## ETL
On top of that, kafka4m provides some basic conveniences for getting data into Kafka from some files in a directory and writing
data from kafka to the local filesystem.

Writing data into can be useful for some performance testing, getting test data in, or just a normal loading of application data.

Reading data from Kafka can be a nice convenience for viewing data locally, or as an interim step to sftp-ing or otherwise uploading the data somewhere.

The Observables provided can just as easily provided data to a multi-part request, a websocket, etc.

### Kafka4mApp 
The 'Kafka4mApp' serves as the entry-point for the ETL jobs and uses the [args4c](https://porpoiseltd.co.uk/args4c/index.html) library.
That simply means that the first argument should be either 'read' or 'write' (as in read data from kafka or write data to kafka), and the 
subsequent args are either key=value pairs or the location of a configuration file.

