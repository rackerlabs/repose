#!/bin/bash

###
# _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
# Repose
# _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
# Copyright (C) 2010 - 2015 Rackspace US, Inc.
# _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
# =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
###

SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
BUILD_DIR=${SCRIPT_DIR}/build
VAGRANT_DIR=${BUILD_DIR}/Vagrant
cd ${SCRIPT_DIR}

# Clean & Build
gradle clean
gradle buildDeb buildRpm -Prelease
rm -f ${BUILD_DIR}/Files_DEB.out ${BUILD_DIR}/Files_RPM.out &&

# Get the .deb's
export VAGRANT_DIR_DEB=${VAGRANT_DIR}/deb
mkdir -p ${VAGRANT_DIR_DEB} &&
rm -f ${VAGRANT_DIR_DEB}/*.deb &&
cp -f $(find . -name Vagrant -prune -o -name "*.deb" -print) ${VAGRANT_DIR_DEB}/ &&
for file in ${VAGRANT_DIR_DEB}/*.deb ; do
   dpkg --contents $file | tr -s ' ' | cut -d' ' -f1,2,6 ;
done | \
sort --key=3 --unique | \
sed 's/repose\/repose \./repose \/ repose /g' | \
sed 's/root\/root \./  root \/   root /g' | \
sed 's/\/$//g' > ${BUILD_DIR}/Files_DEB.out &&

# Get the .rpm's
export VAGRANT_DIR_RPM=${VAGRANT_DIR}/rpm
mkdir -p ${VAGRANT_DIR_RPM} &&
rm -f ${VAGRANT_DIR_RPM}/*.rpm &&
cp -f $(find . -name Vagrant -prune -o -name "*.rpm" -print) ${VAGRANT_DIR_RPM}/ &&
for file in ${VAGRANT_DIR_RPM}/*.rpm ; do
   rpm --query --queryformat '[%{FILEMODES:perms} %6{FILEGROUPNAME} / %6{FILEUSERNAME} %{FILENAMES}\n]' --package $file ;
done | \
sort --key=5 --unique > ${BUILD_DIR}/Files_RPM.out &&

# Do the diff
sdiff ${BUILD_DIR}/Files_DEB.out ${BUILD_DIR}/Files_RPM.out | grep -ve "^d.*<$"

# Copy over the Vagrant files and fake services
cp -R ${SCRIPT_DIR}/src/vagrant/* ${VAGRANT_DIR}/
