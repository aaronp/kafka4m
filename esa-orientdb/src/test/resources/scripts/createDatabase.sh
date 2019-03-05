#!/usr/bin/env bash

source dockerFunctions.sh

docker exec -it ${IMAGE_NAME} /orientdb/bin/console.sh CREATE DATABASE remote:localhost/test root rootpwd PLOCAL
docker exec -it ${IMAGE_NAME} /orientdb/bin/console.sh LIST DATABASES remote:localhost/test