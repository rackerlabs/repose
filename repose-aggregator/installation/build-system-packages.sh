#!/bin/bash

SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
cd ${SCRIPT_DIR}

# Clean and build
mvn clean install -P build-system-packages &&
rm -f Files_DEB.out Files_RPM.out &&

# Get the .deb's
export VAGRANT_DIR_DEB=${SCRIPT_DIR}/deb/target/Vagrant
mkdir -p ${VAGRANT_DIR_DEB} &&
rm -f ${VAGRANT_DIR_DEB}/*.deb &&
cp -f deb/repose-*/target/repose-*.deb ${VAGRANT_DIR_DEB}/ &&
for file in ${VAGRANT_DIR_DEB}/*.deb ; do
   dpkg --contents $file | tr -s ' ' | cut -d' ' -f1,2,6 ;
done | \
sort --key=3 --unique | \
sed 's/repose\/repose \./repose \/ repose /g' | \
sed 's/root\/root \./  root \/   root /g' | \
sed 's/\/$//g' > Files_DEB.out &&

# Get the .rpm's
export VAGRANT_DIR_RPM=${SCRIPT_DIR}/rpm/target/Vagrant
mkdir -p ${VAGRANT_DIR_RPM} &&
rm -f  ${VAGRANT_DIR_RPM}/*.rpm &&
cp -f rpm/repose-*/target/rpm/repose-*/RPMS/noarch/repose-*.noarch.rpm ${VAGRANT_DIR_RPM}/ &&
for file in ${VAGRANT_DIR_RPM}/*.rpm ; do
   rpm --query --queryformat '[%{FILEMODES:perms} %6{FILEGROUPNAME} / %6{FILEUSERNAME} %{FILENAMES}\n]' --package $file ;
done | \
sort --key=5 --unique > Files_RPM.out &&

# Do the diff
sdiff Files_DEB.out Files_RPM.out | grep -ve "^d.*<$"
