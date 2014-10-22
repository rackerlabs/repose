#!/bin/bash
sudo cp -f /etc/repose/system-model.cfg.xml /vagrant/system-model.cfg.xml_ORIG
sudo patch /etc/repose/system-model.cfg.xml < /vagrant/repose-test_system-model.cfg.patch
sudo pip install gunicorn
sudo pip install httpbin
sudo gunicorn httpbin:app &
sudo rm -f /var/log/repose/*.log
sudo service repose-valve start
echo -en "\\nWaiting for Repose to be ready ..."
READY=0
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1
   if [ "$?" -eq 0 ]
   then
      READY=1
   else
      echo -n " ."
      sleep 1
   fi
done
echo -e "\\n\\nRepose is ready."
rm -f /vagrant/repose-curl.out
for i in {1..11} ; do
  echo -e "\\n\\n~~~~~ Attempt #$i ~~~~~\\n\\n" >> /vagrant/repose-curl.out
  curl -H "x-pp-user: abc123" -H "Content-Type: Test" -H "Content-Length: 0" localhost:8080/get -v >> /vagrant/repose-curl.out 2>&1
done
sudo cp -f /var/log/repose/current.log /vagrant/
sudo shutdown -h now
