#!/usr/bin/env bash

source dockerFunctions.sh

stopMongo

docker rm "$IMAGE_NAME"

docker volume rm "$VOLUME_NAME"
docker volume rm "$BACKUP_VOLUME_NAME"