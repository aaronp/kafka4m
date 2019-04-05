#!/usr/bin/env bash

DIR=`dirname $0`
source "$DIR/"dockerFunctions.sh

ensureRunning

docker exec "$IMAGE_NAME" bash -c 'mongo < /data/mount/createUser.js'
