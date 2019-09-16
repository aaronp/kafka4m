# Testing


## testing upload/download locally

This project uses [dockerenv](https://github.com/aaronp/dockerenv/#dockerenv) to run the tests against a 'real' running kafka instance.

When testing, either manually or via automated tests, it can be helpful to inspect the data for a particular topic.

For example, in the Kafka4mAppTest, once data is written to some unique topic, you might want to ssh onto your locally
running kafka instance and inspect the data:


Assuming the Kafka4mApp created topic 'kafka4m-example':

```$xslt
docker exec -it  test-kafka /bin/bash
cd /opt/kafka_2.12-2.2.0/bin/
./kafka-topics.sh --list  --zookeeper localhost:2181 | grep kafka4m-example
./kafka-console-consumer.sh  --topic kafka4m-example  --bootstrap-server localhost:9092 --from-beginning
```

Or simply:

```$xslt
docker exec test-kafka /opt/kafka_2.12-2.2.0/bin/kafka-topics.sh --list  --zookeeper localhost:2181
```

# Testing using docker-compose

The src/test/resources/docker-compose-kafka holds a clone of the wonderful kafka docker-compose work by [wurstmeister/kafka-docker](https://github.com/wurstmeister/kafka-docker)

The docker-compose.yml there has been modified to include the Kafka4m ETL job. Assuming we've run:

```$xslt
sbt docker
```

to build the kafka4m image, we can then run:

```$xslt
cd docker-compose-kafka/
docker-compose  up -d 
``` 

Which will start zookeeper/kafka, then run an ETL job which writes the contents of that example directory into kafka
under the topic 'kafka4m-example'.

We can then verify that by executing:
```$xslt
docker exec  docker-compose-kafka_kafka_1 /opt/kafka_2.12-2.3.0/bin/kafka-topics.sh --list  --zookeeper zookeeper:2181
```

to see the topic, or to see the imported records:
```$xslt
docker exec docker-compose-kafka_kafka_1 \
  /opt/kafka_2.12-2.3.0/bin/kafka-console-consumer.sh \
  --topic kafka4m-example \
  --bootstrap-server \
  localhost:9092 \
  --from-beginning
```

Where you should see the contents of the imported data 


# Testing On a Kafka Cluster
In a similar way to our docker-compose example, you could fork kubernetes-kafka as [I've done here](https://github.com/aaronp/kubernetes-kafka)
and from a gcloud shell run:

```$xslt
git clone https://github.com/aaronp/kubernetes-kafka .
cd kubernetes-kafka

kubectl apply -f namespace
kubectl apply -f rbac-namespace-default
kubectl apply -f zookeeper
kubectl apply -f kafka
```

And then add/run a kafka4m ETL pod, either as a side-car to the kafka container or in a separate pod.