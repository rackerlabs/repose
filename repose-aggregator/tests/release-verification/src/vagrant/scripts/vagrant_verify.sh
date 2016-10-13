#!/bin/bash

echo "~~~TRUNCATED~~~" > /vagrant/validation.log 2>&1
cp /vagrant/scripts/isReposeReady.sh /tmp/
chmod a+x /tmp/isReposeReady.sh
/tmp/isReposeReady.sh /vagrant/var-log-repose-current.log
if [ $? -eq 0 ]; then
   curl -vs http://localhost:8080/resource/this-is-an-id > /vagrant/validation.log 2>&1
fi
grep -qs "200 OK" /vagrant/validation.log
STATUS=$?
if [ "$STATUS" -ne 0 ]; then
   cat /vagrant/validation.log
   echo -en "\n\n"
fi
exit $STATUS
