#!/bin/bash

START=$(date +"%s")
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
REPOSE_DIR=`readlink -f ${SCRIPT_DIR}/../../../../../../`
SOURCE_DIR=`readlink -f ${REPOSE_DIR}/repose-aggregator/tests/release-verification/src`
BUILD_DIR=`readlink -f ${REPOSE_DIR}/repose-aggregator/tests/release-verification/build`

mkdir -p ${BUILD_DIR}
cd ${BUILD_DIR}

if [[ -n $1 && -d "${SOURCE_DIR}/release-verification/${1}" ]] ; then
    cp -R ${SOURCE_DIR}/release-verification/${1} \
          ${BUILD_DIR}
    VERIFY_DIR=${BUILD_DIR}/${1}
else
    cp -R ${SOURCE_DIR}/release-verification/deb \
          ${BUILD_DIR}
    VERIFY_DIR=${BUILD_DIR}/deb
fi
cd ${VERIFY_DIR}
cp -R ${SOURCE_DIR}/release-verification/scripts \
      ${VERIFY_DIR}/
cp -R ${SOURCE_DIR}/release-verification/fake-services \
      ${VERIFY_DIR}/
mkdir -p ${VERIFY_DIR}/etc_repose
cp ${SOURCE_DIR}/config/* \
   ${VERIFY_DIR}/etc_repose/

vagrant destroy --force
vagrant box update
#export REPOSE_VERSION=local
#export REPOSE_VERSION=current
#export REPOSE_VERSION=7.3.8.0
#export REPOSE_VERSION=8.1.0.0
vagrant up

## Test the default install; should result in a Moved Permanently (301) status.
#vagrant ssh -c '
#cp /release-verification/scripts/isReposeReady.sh /tmp/
#chmod a+x /tmp/isReposeReady.sh
#echo "~~~TRUNCATED~~~" > /release-verification/default.log 2>&1
#/tmp/isReposeReady.sh /var/log/repose/current.log
#if [ $? -eq 0 ]; then
#   curl -vs http://localhost:8080 > /release-verification/default.log 2>&1
#fi
#grep -qs "301 Moved Permanently" /release-verification/default.log
#STATUS=$?
#if [ "$STATUS" -ne 0 ]; then
#   cat /release-verification/default.log
#   echo -en "\n\n"
#fi
#'
#if [ $? -ne 0 ]; then
#   echo -en "\n\n~~~~~ ERROR - Default Install FAILED ~~~~~\n\n"
#else
#   echo -en "\n\n~~~~~ SUCCESS - Default Install SUCCESS ~~~~~\n\n"
#fi

# Test the filter bundle install; should result in an Ok (200) status.
vagrant ssh -c 'sh /release-verification/scripts/vagrant_verify.sh'
if [ $? -ne 0 ]; then
   echo -en "\n\n~~~~~ ERROR - Validation Install FAILED ~~~~~\n\n"
else
   echo -en "\n\n~~~~~ SUCCESS - Validation Install SUCCESS ~~~~~\n\n"
fi

STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTime to complete: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n\n"
vagrant port
echo -en "\n\n"
vagrant ssh
