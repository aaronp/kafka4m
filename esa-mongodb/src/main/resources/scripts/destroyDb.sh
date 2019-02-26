#!/usr/bin/env bash

source dockerFunctions.sh

docker stop "$IMAGE_NAME"

docker rm "$IMAGE_NAME"

docker volume rm "$VOLUME_NAME"