#!/bin/bash

JAVA_REPOSE_CLI="/usr/share/lib/repose/repose-cli.jar"

function printUsage() {
   echo -e "Deletes a cache object from Repose's distributed datastore.\n\nUsage: dd-remove.sh <remote-host-address> <cache-key-string>"
}

function dropCacheKey() {
   ENCODED_CACHE_KEY=`java -jar ${JAVA_REPOSE_CLI} dist-datastore encode-key "${2}"`

   echo "Calling remote cache endpoint to remove key: $ENCODED_CACHE_KEY"

   curl -v -X DELETE -H "X-PP-Host-Key: temp" "${1}/powerapi/dist-datastore/objects/${ENCODED_CACHE_KEY}"
}

if [ ${#} -eq 2 ]; then
   dropCacheKey ${@}
else
   printUsage
fi
