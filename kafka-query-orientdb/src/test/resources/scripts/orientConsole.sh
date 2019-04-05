#!/usr/bin/env bash

source dockerFunctions.sh

URL="http://localhost:2480/studio/index.html"
[[ -x ${BROWSER} ]] && exec "${BROWSER}" "$URL"
path=$(which xdg-open || which gnome-open) && exec "$path" "$URL"

docker exec -it ${IMAGE_NAME} /orientdb/bin/console.sh

