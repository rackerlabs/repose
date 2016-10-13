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

vagrant destroy --force
vagrant box update
#export REPOSE_VERSION=local
#export REPOSE_VERSION=default
export REPOSE_VERSION=7.3.8.0
vagrant up

# Make sure the script has executable permissions.
vagrant ssh -c '
cp /vagrant/scripts/vagrant_verify.sh /tmp/
chmod a+x /tmp/vagrant_verify.sh
'

# Test the default install; should result in a Moved Permanently (301) status.
vagrant ssh -c '
echo "~~~TRUNCATED~~~" > /vagrant/default.log 2>&1
/tmp/vagrant_verify.sh /var/log/repose/current.log
if [ $? -eq 0 ]; then
   curl -vs http://localhost:8080 > /vagrant/default.log 2>&1
fi
'
grep -qs '301 Moved Permanently' ${VERIFY_DIR}/default.log
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - Default Install FAILED ~~~~~\n\n"
   cat ${VERIFY_DIR}/default.log
   echo -en "\n\n"
else
   echo -en "\n\n~~~~~ SUCCESS - Default Install SUCCESS ~~~~~\n\n"
fi

# Test the filter bundle install; should result in an Ok (200) status.
mkdir -p ${VERIFY_DIR}/etc_repose
cp ${SOURCE_DIR}/config/* \
   ${VERIFY_DIR}/etc_repose/
vagrant ssh -c '
echo "~~~TRUNCATED~~~" > /vagrant/validation.log 2>&1
/tmp/vagrant_verify.sh /vagrant/var-log-repose-current.log /vagrant/etc_repose
if [ $? -eq 0 ]; then
   sleep 15
   curl -vs http://localhost:8080/resource/this-is-an-id > /vagrant/validation.log 2>&1
fi
'
grep -qs '200 OK' ${VERIFY_DIR}/validation.log
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - Validation Install FAILED ~~~~~\n\n"
   cat ${VERIFY_DIR}/validation.log
   echo -en "\n\n"
else
   echo -en "\n\n~~~~~ SUCCESS - Validation Install SUCCESS ~~~~~\n\n"
fi

STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTime to complete: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n\n"
vagrant port
echo -en "\n\n"
vagrant ssh
