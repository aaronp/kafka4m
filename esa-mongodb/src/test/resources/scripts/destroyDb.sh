#!/usr/bin/env bash

DIR=`dirname $0`
source "$DIR/"dockerFunctions.sh

stopMongo

docker rm "$IMAGE_NAME"

docker volume rm "$VOLUME_NAME"