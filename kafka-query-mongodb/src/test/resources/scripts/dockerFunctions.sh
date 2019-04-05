#!/usr/bin/env bash

#
# This script exposes some functions which can be imported in order to start/stop/check we have a database (via
# a docker container) running.
#
# The scripts are a 'test' resource, as they are not mean to be used in production, but rather only via tests or as
# development tools.
#
# The scripts are invoked from code using 'MongoEnv', which is then used by the 'BaseMongoSpec' to ensure a database
# is running before each test.
#
# Alternatively the scripts (e.g. startMongoDocker.sh, stopMongoDocker.sh) can be used manually by developers if
# they're wanting to ensure an environment is running when starting up processes which run on their dev machine.
#

# see https://hub.docker.com/_/mongo
export VOLUME_NAME=${VOLUME_NAME:-mongo-data}
export IMAGE_NAME=${IMAGE_NAME:-esa-mongo}
export MONGO_PORT=${MONGO_PORT:-9010}

# see https://docs.docker.com/storage/volumes/
createVolume () {
	echo "Creating new volume $VOLUME_NAME"
	docker volume create "$VOLUME_NAME"
}

ensureVolume () {
  (docker volume ls | grep "$VOLUME_NAME") || createVolume
}

stopMongo () {
    docker stop "$IMAGE_NAME"
}

dockerRunMongo () {

    echo "Starting docker image $IMAGE_NAME, but first ensuring volume $VOLUME_NAME"

    ensureVolume

    export MONGO_DATA_DIR=${MONGO_DATA_DIR:-$(pwd)/data}
    mkdir -p "$MONGO_DATA_DIR"
    echo "starting mongo w/ MONGO_DATA_DIR set to $MONGO_DATA_DIR"

    # see "Start a container with a volume" in https://docs.docker.com/storage/volumes/
    echo "RUNNING: docker run --rm --name "${IMAGE_NAME}" -p 9010:27017 -v "$(pwd)":/data/mount -v "${VOLUME_NAME}":/data/db -d mongo:4.0"
    docker run --rm --name "$IMAGE_NAME" -p "${MONGO_PORT}":27017 -v "$(pwd)":/data/mount -v "$VOLUME_NAME":/data/db -d mongo:4.0
}

# start up the docker mongo container if it's not already running
ensureRunning () {
  isRunning || dockerRunMongo
}

isRunning () {
  (docker ps | grep "$IMAGE_NAME") && echo "docker image "${IMAGE_NAME}" is running"
}
