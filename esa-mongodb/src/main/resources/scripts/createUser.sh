#!/usr/bin/env bash

source dockerFunctions.sh

ensureRunning

docker exec "$IMAGE_NAME" bash -c 'mongo < /data/mount/createUser.js'
