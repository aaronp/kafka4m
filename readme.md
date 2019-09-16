Kafka4m
====

[![Build Status](https://travis-ci.org/aaronp/kafka4m.svg?branch=master)](https://travis-ci.org/aaronp/kafka4m)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.12/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.12)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/kafka4m/badge.svg?branch=master)](https://coveralls.io/github/aaronp/kafka4m?branch=master)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.12)

### Kafka for  Monix

An opinionated library to expose Kafka as an Observable and Observer of data.

It is opinionated in that it also uses the typesafe config (via [args4c](https://github.com/aaronp/args4c)) as a means to drive the kafka config.


```$xslt
  val config = ConfigFactory.load()

  // write data to kafka (assumes a configuration akin to e.g. kafka4m.topic = someNewTopic)
  val kafkaWriter: Consumer[(String, Array[Byte]), Long] = kafka4m.writeKeyAndBytes(config)

  // read data from kafka (assumes a configuration such as kafka4m.consumer.topic = originalTopic)
  val kafkaData: Observable[ConsumerRecord[String, Array[Byte]]] = kafka4m.read(config)

  // then we'd write it back into kafka like this.
  val task: Task[Long] = kafkaData.map(r => (r.key, r.value)).consumeWith(kafkaWriter)
```  

In addition to those few simple functions, kafka4m includes basic io and etl subpackages which serve not only to help as further documentation and performance-test
kafka set-ups, but also as a practical and performant means to get data into and out of kafka.  

The minisite can be found [here](https://aaronp.github.io/kafka4m/index.html)
