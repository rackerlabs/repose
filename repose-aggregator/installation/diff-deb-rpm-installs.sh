#!/bin/bash

SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
cd ${SCRIPT_DIR}

# Clean and build
mvn clean install -P build-system-packages &&
rm -f Files_DEB.out Files_RPM.out &&

# Get the .deb's
mkdir -p deb/Vagrant &&
rm -f deb/Vagrant/*.deb &&
cp -f deb/repose-*/target/repose-*.deb deb/Vagrant/ &&
for file in deb/Vagrant/*.deb ; do
   dpkg --contents $file | tr -s ' ' | cut -d' ' -f1,2,6 ;
done | \
sort --key=3 --unique | \
sed 's/repose\/repose \./repose \/ repose /g' | \
sed 's/root\/root \./  root \/   root /g' | \
sed 's/\/$//g' > Files_DEB.out &&

# Get the .rpm's
mkdir -p rpm/Vagrant &&
rm -f  rpm/Vagrant/*.rpm &&
cp -f rpm/repose-*/target/rpm/repose-*/RPMS/noarch/repose-*.noarch.rpm rpm/Vagrant/ &&
for file in rpm/Vagrant/*.rpm ; do
   rpm --query --queryformat '[%{FILEMODES:perms} %6{FILEGROUPNAME} / %6{FILEUSERNAME} %{FILENAMES}\n]' --package $file ;
done | \
sort --key=5 --unique > Files_RPM.out &&

# Do the diff
sdiff Files_DEB.out Files_RPM.out | grep -ve "^d.*<$"