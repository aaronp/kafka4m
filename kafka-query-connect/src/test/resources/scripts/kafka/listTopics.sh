#!/usr/bin/env bash
# see https://hub.docker.com/r/spotify/kafka

DIR=`dirname $0`
source "$DIR/"dockerFunctions.sh

echo "topics for $IMAGE_NAME are..."
docker exec -t "$IMAGE_NAME" /opt/kafka_2.11-0.10.1.0/bin/kafka-topics.sh --list --zookeeper localhost:2181