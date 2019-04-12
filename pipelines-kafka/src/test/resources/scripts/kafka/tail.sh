#!/usr/bin/env bash

DIR=`dirname $0`
source "$DIR/"dockerFunctions.sh

topic=$1
echo "tailing $topic in $IMAGE_NAME"

docker exec -t "$IMAGE_NAME" /opt/kafka_2.11-0.10.1.0/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic "$topic" \
  --from-beginning
