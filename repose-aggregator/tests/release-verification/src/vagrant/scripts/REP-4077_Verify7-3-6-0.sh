#!/bin/bash

START=$(date +"%s")
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
REPOSE_DIR=`readlink -f ${SCRIPT_DIR}/../../../../../../`
SOURCE_DIR=`readlink -f ${REPOSE_DIR}/repose-aggregator/tests/release-verification/src`
BUILD_DIR=`readlink -f ${REPOSE_DIR}/repose-aggregator/tests/release-verification/build/Vagrant`

mkdir -p ${BUILD_DIR}
cd ${BUILD_DIR}
cp -R ${SOURCE_DIR}/vagrant/* \
      ${BUILD_DIR}

if [[ -n $1 && -d "${BUILD_DIR}/${1}" ]] ; then
    VERIFY_DIR=${BUILD_DIR}/${1}
else
    VERIFY_DIR=${BUILD_DIR}/deb
fi
cd ${VERIFY_DIR}
cp -R ${BUILD_DIR}/scripts \
      ${VERIFY_DIR}/
cp -R ${BUILD_DIR}/fake-services \
      ${VERIFY_DIR}/

vagrant destroy --force &&
vagrant box update &&
#export REPOSE_VERSION=local &&
#export REPOSE_VERSION=default &&
export REPOSE_VERSION=7.3.8.0 &&
vagrant up &&
vagrant ssh -c 'echo "~~~TRUNCATED~~~" > /vagrant/default.log 2>&1
/vagrant/scripts/isReposeReady.sh /var/log/repose/current.log
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
else
   echo -en "\n\nRepose is ready.\n"
   sleep 3
   curl -vs http://localhost:8080 > /vagrant/default.log 2>&1
fi'
mkdir -p ${VERIFY_DIR}/etc_repose &&
cp ${SOURCE_DIR}/config/* \
   ${VERIFY_DIR}/etc_repose/ &&
vagrant ssh -c 'echo "~~~TRUNCATED~~~" > /vagrant/var-log-repose-current.log  2>&1
sudo mkdir -p /etc/repose/orig
sudo sh -c "cp /etc/repose/*.* /etc/repose/orig/"
sudo cp /vagrant/etc_repose/*.* /etc/repose/
echo "~~~TRUNCATED~~~" > /vagrant/validation.log 2>&1
/vagrant/scripts/isReposeReady.sh /vagrant/var-log-repose-current.log
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
else
   echo -en "\n\nRepose is ready.\n"
   sleep 3
   curl -vs http://localhost:8080/resource/this-is-an-id > /vagrant/validation.log 2>&1
fi'
cat ${VERIFY_DIR}/default.log
echo -en "\n\n"
cat ${VERIFY_DIR}/validation.log
STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTime to complete: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n\n"
vagrant port
echo -en "\n\n"
vagrant ssh
