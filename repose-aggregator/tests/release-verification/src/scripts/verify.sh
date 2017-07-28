#!/bin/bash

echo "-------------------------------------------------------------------------------------------------------------------"
echo "Starting Mock Services"
echo "-------------------------------------------------------------------------------------------------------------------"
sh /release-verification/scripts/fake_keystone_run.sh
sh /release-verification/scripts/fake_origin_run.sh
sh /release-verification/scripts/repose_run_docker.sh

echo "~~~TRUNCATED~~~" > /release-verification/validation.log 2>&1
/bin/bash /release-verification/scripts/isReposeReady_docker.sh /release-verification/var-log-repose-current.log
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
