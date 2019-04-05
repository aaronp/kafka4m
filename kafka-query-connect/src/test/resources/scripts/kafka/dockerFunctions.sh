#!/usr/bin/env bash

#
# This script exposes some functions which can be imported in order to start/stop/check we have a our environment up.
#
# The scripts are a 'test' resource, as they are not mean to be used in production, but rather only via tests or as
# development tools.
#
# The scripts are invoked from code using 'KafkaEnv', which is then used by the 'BaseSpec' to ensure a database
# is running before each test.
#
# Alternatively the scripts (e.g. startDocker.sh, stopDocker.sh) can be used manually by developers if
# they're wanting to ensure an environment is running when starting up processes which run on their dev machine.
#

export VOLUME_NAME=${VOLUME_NAME:-kafka-data}
export IMAGE_NAME=${IMAGE_NAME:-test-kafka}

# see https://docs.docker.com/storage/volumes/
createVolume () {
	echo "Creating new volume $VOLUME_NAME"
	docker volume create "$VOLUME_NAME"
}

ensureVolume () {
  (docker volume ls | grep "$VOLUME_NAME") || createVolume
}

stopKafka() {
    docker stop "$IMAGE_NAME"
}

dockerRunKafka() {
    echo "Starting docker image $IMAGE_NAME, but first ensuring volume $VOLUME_NAME"

    ensureVolume

    # see https://hub.docker.com/r/spotify/kafka and https://rmoff.net/2018/08/02/kafka-listeners-explained/ for exposing ADVERTISED_LISTENERS and ADVERTISED_HOST
    docker run --rm -t -d -h kafka0 --name "$IMAGE_NAME" \
      -p 2181:2181 -p 9092:9092 \
      --env ADVERTISED_HOST=localhost \
      --env ADVERTISED_PORT=9092 \
      --env KAFKA_ADVERTISED_LISTENERS=LISTENER_BOB://kafka0:29092,LISTENER_FRED://localhost:9092 \
      --env KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=LISTENER_BOB:PLAINTEXT,LISTENER_FRED:PLAINTEXT \
      spotify/kafka
}

# start up the docker Kafkacontainer if it's not already running
ensureRunning () {
  isRunning || dockerRunKafka
}

isKafkaRunning () {
  (docker ps | grep "$IMAGE_NAME") && echo "docker image "${IMAGE_NAME}" is running"
}
