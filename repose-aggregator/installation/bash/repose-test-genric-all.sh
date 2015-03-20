#!/bin/bash

###
# #%L
# Repose
# %%
# Copyright (C) 2010 - 2015 Rackspace US, Inc.
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###
sudo cp -f /etc/repose/system-model.cfg.xml /vagrant/system-model.cfg.xml_ORIG
sudo patch /etc/repose/system-model.cfg.xml < /vagrant/repose-test_system-model.cfg.patch
sudo pip install gunicorn
sudo pip install httpbin
sudo gunicorn httpbin:app &
sudo rm -f /var/log/repose/*.log
sudo service repose-valve start
echo -en "\\nWaiting for Repose to be ready ..."
READY=0
COUNT=0
TIMEOUT=30
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1
   if [ "$?" -eq 0 ]; then
      READY=1
   else
      let "COUNT +=1"
      if [ "$COUNT" -ge "$TIMEOUT" ]; then
         echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START ~~~~~\n\n"
         break
      fi
      echo -n " ."
      sleep 1
   fi
done
if [ $READY -eq 0 ]; then
    sudo shutdown -h now
    exit 5
else
    echo -e "\\n\\nRepose is ready."
    rm -f /vagrant/repose-curl.out
    for i in {1..11} ; do
      echo -e "\\n\\n~~~~~ Attempt #$i ~~~~~\\n\\n" >> /vagrant/repose-curl.out
      curl -H "x-pp-user: abc123" -H "Content-Type: Test" -H "Content-Length: 0" localhost:8080/get -v >> /vagrant/repose-curl.out 2>&1
    done
    sudo cp -f /var/log/repose/current.log /vagrant/
    sudo shutdown -h now
fi
