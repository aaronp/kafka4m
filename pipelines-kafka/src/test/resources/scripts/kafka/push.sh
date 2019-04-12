#!/usr/bin/env bash

DIR=`dirname $0`
source "$DIR/"dockerFunctions.sh

topic="$1"
echo "write std-in to $topic"

docker exec -t "$IMAGE_NAME" /opt/kafka_2.11-0.10.1.0/bin/kafka-console-producer.sh \
   --broker-list localhost:9092 \
   --topic "$topic"
