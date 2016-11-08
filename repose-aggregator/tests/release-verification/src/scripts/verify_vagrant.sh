#!/bin/bash

echo "~~~TRUNCATED~~~" > /release-verification/validation.log 2>&1
cp /release-verification/scripts/isReposeReady_vagrant.sh /tmp/
chmod a+x /tmp/isReposeReady_vagrant.sh
/tmp/isReposeReady_vagrant.sh /release-verification/var-log-repose-current.log
if [ $? -eq 0 ]; then
   curl -vs http://localhost:8080/resource/this-is-an-id > /release-verification/validation.log 2>&1
fi
grep -qs "200 OK" /release-verification/validation.log
STATUS=$?
if [ "$STATUS" -ne 0 ]; then
   cat /release-verification/validation.log
   echo -en "\n\n"
fi
exit $STATUS
