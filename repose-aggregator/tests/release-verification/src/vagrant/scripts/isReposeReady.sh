#!/bin/bash

if [[ -n $1 && -f "${1}" ]] ; then
    LOG_FILE="${1}"
else
    LOG_FILE="/var/log/repose/current.log"
fi

echo -en "\nWaiting for Repose to be ready ..."
READY=0
COUNT=0
TIMEOUT=60
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" $LOG_FILE >> /dev/null 2>&1
   if [ "$?" -eq 0 ]; then
      READY=1
   else
      let "COUNT +=1"
      if [ "$COUNT" -ge "$TIMEOUT" ]; then
         break
      fi
      echo -n " ."
      sleep 1
   fi
done
if [ $READY -eq 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
fi
