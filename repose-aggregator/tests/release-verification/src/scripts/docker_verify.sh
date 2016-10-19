#!/bin/bash

echo "-------------------------------------------------------------------------------------------------------------------"
echo "Starting Mock Services"
echo "-------------------------------------------------------------------------------------------------------------------"
sh /scripts/fake_keystone_run.sh
sh /scripts/fake_origin_run.sh

echo "~~~TRUNCATED~~~" > /validation.log 2>&1
cp /scripts/isReposeReady.sh /tmp/
chmod a+x /tmp/isReposeReady.sh
/tmp/isReposeReady.sh /var-log-repose-current.log
if [ $? -eq 0 ]; then
   curl -vs http://localhost:8080/resource/this-is-an-id > /validation.log 2>&1
fi
grep -qs "200 OK" /validation.log
STATUS=$?
if [ "$STATUS" -ne 0 ]; then
   cat /validation.log
   echo -en "\n\n"
fi
exit $STATUS
