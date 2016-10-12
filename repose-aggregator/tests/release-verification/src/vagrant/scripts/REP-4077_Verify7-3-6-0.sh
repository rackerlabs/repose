#!/bin/bash

START=$(date +"%s") &&
cd ~/Projects/repose &&
mkdir -p ./repose-aggregator/artifacts/build/Vagrant/ &&
cd ./repose-aggregator/artifacts/build/Vagrant/ &&
cp -R ../../src/vagrant/* \
      ./ &&

if [[ -n $1 && -d "${1}" ]] ; then
    cd ${1}
else
    cd ./deb
fi

vagrant destroy --force &&
vagrant box update &&
#export REPOSE_VERSION=local &&
#export REPOSE_VERSION=default &&
export REPOSE_VERSION=7.3.8.0 &&
vagrant up &&
vagrant ssh -c 'echo -en "\nWaiting for Repose to be ready ..."
echo "~~~TRUNCATED~~~" > /vagrant/default.log 2>&1
READY=0
COUNT=0
TIMEOUT=60
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1
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
else
   echo -en "\n\nRepose is ready.\n"
   curl -vs http://localhost:8080 > /vagrant/default.log 2>&1
fi' &&
mkdir -p ./etc_repose &&
cp ../test/common/* \
   ./etc_repose/ &&
cp ../test/REP-4077_Verify7-3-6-0/* \
   ./etc_repose/ &&
vagrant ssh -c 'sudo sh -c "echo "~~~TRUNCATED~~~" > /vagrant/var-log-repose-current.log"  2>&1
sudo mkdir -p /etc/repose/orig
sudo sh -c "cp /etc/repose/*.* /etc/repose/orig/"
sudo cp /vagrant/etc_repose/*.* /etc/repose/
echo -en "\nWaiting for Repose to be ready ..."
echo "~~~TRUNCATED~~~" > /vagrant/validation.log 2>&1
READY=0
COUNT=0
TIMEOUT=60
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /vagrant/var-log-repose-current.log >> /dev/null 2>&1
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
else
   echo -en "\n\nRepose is ready.\n"
   curl -vs http://localhost:8080/resource/this-is-an-id > /vagrant/validation.log 2>&1
fi'
cat default.log
echo -en "\n\n"
cat validation.log
STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTime to complete: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n\n"
vagrant port
echo -en "\n\n"
vagrant ssh
