## Usage

The kafka configurations are based on the the kafka4m typesafe configuration. In practice the intent is to achieve similar things
to what Kafka Streams gives you (but w/o relying on the kafka streams API)

## Copy A Kafka Topic (i.e. Mirror Maker):  
```$xslt
  val config = ConfigFactory.load()

  // write data to kafka (assumes a configuration akin to kafka4m.producer.topic = someNewTopic)
  val kafkaWriter: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)

  // read data from kafka (assumes a configuration akin to kafka4m.consumer.topic = originalTopic)
  val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.readRecords(config)

  // then we'd write it back into kafka like this.
  val task: Task[Long] = kafkaData.map(r => (r.key, r.value)).consumeWith(kafkaWriter)
``` 

That provides the base primitives -- getting data into and out of Kafka.

## Manually Acking

By default Kafka just commits paritions/offsets periodically. To give client code more control over that, kafka4m sets the default of 'enable.auto.commit' to false.

It does this via:
```
kafka4m.consumer.enable.auto.commit : false
```

Which you can of course change.

The idea though is that a 'kafka4m.reader' returns a stream of 'AckableRecords', which is a wrapper around the ConsumerRecord which exposes a 'persistOffset' function which allows you to persist the current offset partitions back to Kafka.

A very basic example would be something like this:

```
    // our custom data type
    case class MyData(id : String, x: Int)

    // a means to unmarshall it from a record
    def unmarshal(bytes :Array[Byte]) : MyData = ???

    // some method to persist our data
    def writeToDB(value : MyData) : Future[Boolean] = ???

    kafka4m.read().map { ackable: AckableRecord[ConsumerRecord[String, Array[Byte]]] =>
      val data = unmarshal(ackable.record.value())
      val commitFuture: Future[Map[TopicPartition, OffsetAndMetadata]] = writeToDB(data).flatMap(_ => ackable.commitPosition())
      commitFuture.onComplete(x => logger.info("Committed: " + x))
      data
    } 
```

## ETL
On top of that, kafka4m provides some basic conveniences for getting data into Kafka from the filesystem and writing
data from kafka to the filesystem.

### Kafka4mApp 
The 'Kafka4mApp' serves as the entry-point for the ETL jobs and uses the [args4c](https://porpoiseltd.co.uk/args4c/index.html) library.
That simply means that the first argument should be either 'read' or 'write' (as in read data from kafka or write data to kafka), and the 
subsequent args are either key=value pairs or the location of a configuration file.


### As a docker image
Aside from being able to extend it in your project, it also works just out-of-the box, and so we've published a docker image
to do just that:

Write some data into kafka:
```$xslt
echo "example" > ./dataIn/example.txt
docker run kafka4m:latest write kafka4m.etl.intoKafka.dataDir=./dataIn kafka4m.topic=foo
``` 

Write a lot of data into kafka:
```$xslt
docker run kafka4m:latest write \
  kafka4m.etl.intoKafka.dataDir=./dataIn \
  kafka4m.topic=foo \ 
  kafka4m.etl.intoKafka.repeat=true
``` 

Read the data out:
```$xslt
docker run kafka4m:latest read \
  kafka4m.etl.fromKafka.dataDir=./dataOut \
  kafka4m.topic=foo
``` 

There are a lot more configuration options (caching, limits, rate-limiting, etc) for the ETL work. 
Please just consume the reference.conf for options. 