#!/usr/bin/env bash

#
# This script exposes some functions which can be imported in order to start/stop/check we have a database (via
# a docker container) running.
#
# The scripts are a 'test' resource, as they are not mean to be used in production, but rather only via tests or as
# development tools.
#
# The scripts are invoked from code using 'OrientDbEnv', which is then used by the 'BaseOrientDbSpec' to ensure a database
# is running before each test.
#
# Alternatively the scripts (e.g. startOrientDbDocker.sh, stopOrientDbDocker.sh) can be used manually by developers if
# they're wanting to ensure an environment is running when starting up processes which run on their dev machine.
#

# see https://hub.docker.com/_/orientdb
export VOLUME_NAME=${VOLUME_NAME:-orientDb-data}
export BACKUP_VOLUME_NAME=${VOLUME_NAME:-orientDb-data-backup}
export IMAGE_NAME=${IMAGE_NAME:-esa-orientdb}
export ORIENTDB_PORT=${ORIENTDB_PORT:-9020}

# see https://docs.docker.com/storage/volumes/
createVolume () {
	echo "Creating new volume $VOLUME_NAME"
	docker volume create "$VOLUME_NAME"
}
createBackupVolume () {
	echo "Creating new backup volume BACKUP_VOLUME_NAME"
	docker volume create "BACKUP_VOLUME_NAME"
}

ensureVolume () {
  (docker volume ls | grep "$VOLUME_NAME") || createVolume
  (docker volume ls | grep "$BACKUP_VOLUME_NAME") || createBackupVolume
}

stopOrientDb () {
    docker stop "$IMAGE_NAME"
}

dockerRunOrientDb () {

    echo "Starting docker image $IMAGE_NAME, but first ensuring volume $VOLUME_NAME"

    ensureVolume

    export ORIENTDB_DATA_DIR=${ORIENTDB_DATA_DIR:-$(pwd)/data}
    mkdir -p "$ORIENTDB_DATA_DIR"
    echo "starting orientDb w/ ORIENTDB_DATA_DIR set to $ORIENTDB_DATA_DIR"

    # see "Start a container with a volume" in https://docs.docker.com/storage/volumes/


#    ORIENT_CMD="docker run --rm --name $IMAGE_NAME -p 2424:2424 -p 2480:2480 -e ORIENTDB_ROOT_PASSWORD=rootpwd -v $(pwd):/data/mount -v ${VOLUME_NAME}:/orientdb/databases -v ${BACKUP_VOLUME_NAME}:/orientdb/backup -d orientdb:3.0.17"
    ORIENT_CMD="docker run --rm --name $IMAGE_NAME -p 2424:2424 -p 2480:2480 -e ORIENTDB_ROOT_PASSWORD=rootpwd -v $(pwd):/data/mount -v ${VOLUME_NAME}:/orientdb/databases -v ${BACKUP_VOLUME_NAME}:/orientdb/backup -d orientdb:2.2.37"

    echo "Running $ORIENT_CMD"
    exec ${ORIENT_CMD}
}

# start up the docker orientDb container if it's not already running
ensureRunning () {
  isRunning || dockerRunOrientDb
}

isRunning () {
  (docker ps | grep "$IMAGE_NAME") && echo "docker image "${IMAGE_NAME}" is running"
}
