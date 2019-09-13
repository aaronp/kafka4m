# Testing


## testing upload/download locally

This project uses [dockerenv](https://github.com/aaronp/dockerenv/#dockerenv) to run the tests against a 'real' running kafka instance.

When testing, either manually or via automated tests, it can be helpful to inspect the data for a particular topic.

For example, in the Kafka4mAppTest, once data is written to some unique topic, you might want to ssh onto your locally
running kafka instance and inspect the data:


Assuming the Kafka4mApp created topic ABC123:

```$xslt
docker exec -it  test-kafka /bin/bash
cd /opt/kafka_2.12-2.2.0/bin/
./kafka-topics.sh --list  --zookeeper localhost:2181 | grep ABC123
./kafka-console-consumer.sh  --topic ABC123  --bootstrap-server localhost:9092 --from-beginning
```

Or simply:

```$xslt
docker exec test-kafka /opt/kafka_2.12-2.2.0/bin/kafka-topics.sh --list  --zookeeper localhost:2181
```


# Testing On a Kafka Cluster
