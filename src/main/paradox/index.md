Kafka4m
======
[![Build Status](https://travis-ci.org/aaronp/kafka4m.svg?branch=master)](https://travis-ci.org/aaronp/kafka4m)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.12/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.github.aaronp/kafka4m_2.12)
[![Coverage Status](https://coveralls.io/repos/github/aaronp/kafka4m/badge.svg?branch=master)](https://coveralls.io/github/aaronp/kafka4m?branch=master)
[![Scaladoc](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/com.github.aaronp/kafka4m_2.12)

Kafka4m (kafka for monix) is a bit like the [fs2kafka](https://fd4s.github.io/fs2-kafka/) or [zio-kafka](https://github.com/zio/zio-kafka), but a bit more opinionated, in that it also brings in a typesafe configuration and application setup.

It's purpose was to be able to use the [rich set of streaming features provided by monix](https://monix.io/docs/3x/reactive/observable.html) while still having access to the data structures provided by the [kafka consumer api](https://kafka.apache.org/20/javadoc/org/apache/kafka/clients/consumer/Consumer.html).

You can use thinks like 'kafka4m.loadBalance' to execute tasks in parallel while ensuring only the offset for the most-recent successful task is committed.

Check out the latest scaladocs [here](https://aaronp.github.io/kafka4m/api/latest/kafka4m/index.html)

@@toc { depth=1 }

@@@ index

* [Usage](usage.md)
* [Building/Releasing](building.md)
* [Testing](testing.md)
* [License](license.md)

@@@

