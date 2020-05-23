Kafka4m
====

[![Build Status](https://travis-ci.org/aaronp/kafka4m.svg?branch=master)](https://travis-ci.org/aaronp/kafka4m)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.13/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.13)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/kafka4m/badge.svg?branch=master)](https://coveralls.io/github/aaronp/kafka4m?branch=master)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.13.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.13)

### kafka4m: Kafka for Monix

This library was initially created for a few reasons:

 * To expose the underlying Kafka data structures (e.g. ProducerRecords and ConsumerRecords) not available in KSQL
 * To bind the Kafka API with the a library dedicated to performant, back-pressured data streaming and take advantage of the [rich monix API](https://monix.io/api/3.0/monix/reactive/Observable.html) for streaming data
 * In addition to monix, support interop with the [typesafe config](https://github.com/lightbend/config) to get Observables/Consumers "out of the box"
 * To support easy-to-reason-about multi-threaded interaction with Kafka, ensuring offsets/partitions are only committed when safe to do so.
      
This project started as a means to gain the kind of deeper understanding of Kafka and the differences/trade-offs between Kafka Connect, KSQL and the Kafka API.
I wanted access to the full Kafka ProducerRecord/ConsumerRecord data structures afforded in the Kafka API, but with the concurrency and functional streaming capabilities
of a library like monix which is dedicated to those areas. 

# Usage

For these examples, imagine we have our own data structure we want to work with - for this example just a case class: 
```scala
  case class SomeData(key: String, value: String)
```  


## Writing Observable streams to Kafka

If we can provide a means to convert that data structure to a Kafa ProducerRecord:
```scala
trait AsProducerRecord[-A] {
  type K
  type V
  def asRecord(value: A): ProducerRecord[K, V]
```

Then we can expose a [monix consumer](https://monix.io/docs/2x/reactive/consumer.html) which will be able to write an [Observable](https://monix.io/api/3.0/monix/reactive/Observable.html) to Kafka:

```scala
val intoKafka: Consumer[SomeData, Long] = kafka4m.write[SomeData](config)
```

Note: The 'Long' Consumer result is the number of records written, though in practice these tend to work on infinite streams of data.

FYI, the 'AsProducerRecord' typeclass can be created from any function that exposes a key/value pair for your data type: 

```scala
  val topic = "example"
  implicit val asProducer = AsProducerRecord.liftForTopic(topic) { data: SomeData =>
    (data.key, data.value.getBytes())
  }
```

Which also plays nicely with existing Kafka Serde methods such as the [Schema Registry](https://docs.confluent.io/current/schema-registry/schema_registry_tutorial.html).

## Reading data from Kafka

The Kafka consumer API cannot be accessed on multiple threads by design. That is, Kafka enforces single-threaded access to 
the polling of records, and assumes that polling is a kind of liveliness-check. 

This of course makes sense -- imagine the case where you consume two messages A and B from Kafka. You start to persist them both, and B completes first.
Can you commit the offset/partition for B? Because if you do and then discover the persist job failed for A, then you've lost a record.

Having to perform some blocking IO on the single Kafka consumer thread, however, would be a performance killer.
 
For this reason, kafka4m doesn't expose what you would expect, which would be:

```scala
val kafkaData : Observable[ConsumerRecord[K, V]] = ???
```

Instead, it wraps the Kafka ConsumerRecords in an 'AckableRecord', which is a data-structure which exposes the underlying ConsumerRecord, but also has a way to commit those messages back
to Kafka in a thread-safe way.


### Reading basic byte array values
```scala
  val fromKafka: Observable[AckBytes] = kafka4m.readByteArray()

  val keys = fromKafka.zipWithIndex.map {
    case (ackable, i) =>
      if (i % 100 == 0) {
        ackable.commitPosition()
      }
      // use the consumer record, including the timestamp, offset/position, etc
      val kafkaRecord: ConsumerRecord[String, Array[Byte]] = ackable.record
      println(kafkaRecord.headers())
      println(kafkaRecord.timestamp())
      
      kafkaRecord.key()
  }

```

NOTE: The 'readByteArray' is using a default typesafe config - the equivalent of:

```scala
val config : Config = ConfigFactory.load()
val fromKafka: Observable[AckableRecord[ConsumerRecord[String, Array[Byte]]]] = kafka4m.readByteArray(config)
```

And the kafka consumer configuration is taken from the typesafe config path 'kafka4m.consumer.XXX', where XXX is taken to be any supported Kafka key/value pairs:

```scala
kafak4m.consumer.bootstrap.servers : kafkahost:9091
kafak4m.consumer.topic : foo
kafak4m.consumer.auto.offset.reset : earliest
```

### Reading Typed Records

In addition to the raw ConsumerRecord, kafka4m uses the 'RecordDecoder' typeclass as a way to deserialize ConsumerRecords into other types.

For example, if for our 'SomeData' type we have:

```scala
  implicit val decoder  = RecordDecoder.forBytes { bytes =>
    val i = new String(bytes).toInt
    SomeData(s"key-$i", i.toString)
  }
``` 

Then we can get either an Observable of AckableRecords for SomeData using 'kafka4m.read':

```scala
  val fromKafka: Observable[AckableRecord[SomeData]] = kafka4m.read[SomeData]()
```

Or just the simpler 'Observable[SomeData] using 'kafka4m.readRecords':
```scala
  val fromKafka: Observable[SomeData] = kafka4m.readRecords[SomeData]()
```


### Performant multi-threaded use

As a software engineer, features like "auto commit" make me nervous. Simply because a process is consuming records doesn't necessarily mean they're safely persisted.

Consider the case when a process consumes ten messages. It should be able to reliably persist those ten messages concurrently, only committing the Kafka offset/partitions when it's safe to do so.

By representing the persisting/handling of a Kafka record simply as a [monix Task](https://monix.io/docs/2x/eval/task.html), kafka4m can keep track of and persist offsets/partitions for the furthest successfully completed task.

Essentially for each kafka record 'A' received from Kafka, we want to kick off a Task[B]. When Task[B] completes successfully, 
kafka4m takes that as a signal it's safe to commit the offset/partition for A:

```scala
recordHandler : A => Task[B]
```

Kafka4m users can use the full power of [the monix Task API](https://monix.io/docs/2x/eval/task.html) for retrying, recovering, etc.

Given that and how nicely monix plays with other IO librariess (cats IO, ZIO, etc), this allows us to use the full expressive power of reactive-streams, Observables and IO side-effects with Kafka.

For example, let's just use a [cats REF](https://typelevel.org/cats-effect/api/cats/effect/concurrent/Ref.html) for a simple map-backed in-memory database:

```scala
  import cats.effect.concurrent.Ref
  val database: Ref[Task, Map[String, SomeData]] = Ref.unsafe[Task, Map[String, SomeData]](Map.empty[String, SomeData])
 
  // using kafka4m.loadBalance gives us an infinite stream (represented as a monix Observable] of our ComputeResults
  val computeResults: Observable[ComputeResult[SomeData, Unit]] = kafka4m.loadBalance[SomeData, Unit]() { next: SomeData =>
  
    // this is our "persist this record to a database" thunk -- here just using our Ref to give us a Task that'll update our map:
    database.update { map =>
      map.updated(next.key, next)
    }
  }
```  

Our 'computeResults' Observable will, when consumed, connect to Kafka and execute that closure across the threads currently available, 
only committing the partition/offsets for successfully completed Tasks.

The 'kafka4m.loadBalance' takes a typesafe config and parallelism factor as args amongst other things, but all with sensible defaults.

# Further Documentation

The minisite can be found [here](https://aaronp.github.io/kafka4m/index.html)
