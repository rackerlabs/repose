#!/bin/bash

if [[ -n $1 && -f "${1}" ]] ; then
    LOG_FILE="${1}"
else
    LOG_FILE="/var/log/repose/current.log"
fi
#sudo sh -c "echo '~~~~~ TRUNCATED ~~~~~' > ${LOG_FILE}  2>&1"
if [[ -n $2 && -d "${2}" ]] ; then
    sudo cp --force ${2}/* /etc/repose/
fi
cp /vagrant/scripts/isReposeReady.sh /tmp/
chmod a+x /tmp/isReposeReady.sh
/tmp/isReposeReady.sh ${LOG_FILE}
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
else
   echo -en "\n\nRepose is ready.\n"
   sleep 3
fi
