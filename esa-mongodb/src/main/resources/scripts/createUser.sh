#!/usr/bin/env bash

source dockerFunctions.sh

ensureRunning

docker exec some-mongo bash -c 'mongo < /data/mount/createUser.js'
