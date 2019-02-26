#!/usr/bin/env bash
# see https://hub.docker.com/_/mongo

export VOLUME_NAME=${VOLUME_NAME:-mongo-data}
export IMAGE_NAME=${IMAGE_NAME:-esa-mongo}

# see https://docs.docker.com/storage/volumes/
createVolume () {
	echo "Creating new volume $VOLUME_NAME"
	docker volume create "$VOLUME_NAME"
}

ensureVolume () {
  (docker volume ls | grep "$VOLUME_NAME") || createVolume
}

dockerRunMongo () {

    echo "Starting docker image $IMAGE_NAME, but first ensuring volume $VOLUME_NAME"

    ensureVolume

    export MONGO_DATA_DIR=${MONGO_DATA_DIR:-$(pwd)/data}
    mkdir -p "$MONGO_DATA_DIR"
    echo "starting mongo w/ MONGO_DATA_DIR set to $MONGO_DATA_DIR"

    echo "RUNNING: docker run --name "$IMAGE_NAME" -p 27017:9010 -v "$(pwd)":/data/mount -v "$VOLUME_NAME":/data/db -d mongo:4.0"

    # see "Start a container with a volume" in https://docs.docker.com/storage/volumes/
    docker run --name "$IMAGE_NAME" -p 27017:9010 -v "$(pwd)":/data/mount -v "$VOLUME_NAME":/data/db -d mongo:4.0

}

ensureRunning () {
  (docker ps | grep "$IMAGE_NAME") || dockerRunMongo
}
