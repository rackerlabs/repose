#!/bin/bash

pushd `pwd`
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
#export REPOSE_DIR=${SCRIPT_DIR}/../../../
export VAGRANT_DIR=${SCRIPT_DIR}/Vagrant
export PATCH_DIR=${SCRIPT_DIR}/../bash
#cd ${REPOSE_DIR}/
#mvn clean install -DskipTests=true
cd ${SCRIPT_DIR}
#mvn clean install -P build-system-packages
mkdir -p ${VAGRANT_DIR}
cp -f         repose-test-genric-rpm.sh ${VAGRANT_DIR}/
cp -f ${PATCH_DIR}/repose-test-genric-all.sh ${VAGRANT_DIR}/
cd ${VAGRANT_DIR}
rm -f Vagrantfile repose-*.noarch.rpm
cp -f ${SCRIPT_DIR}/repose-*/target/rpm/repose-*/RPMS/noarch/repose-*.noarch.rpm ${VAGRANT_DIR}/
cp -f ${PATCH_DIR}/repose-test_system-model.cfg.patch ${VAGRANT_DIR}/
vagrant init centos-65-x64-virtualbox-puppet http://puppet-vagrant-boxes.puppetlabs.com/centos-65-x64-virtualbox-puppet.box
vagrant up

vagrant ssh -c "/vagrant/repose-test-genric-rpm.sh && /vagrant/repose-test-genric-all.sh"
vagrant destroy -f
echo -e "\n\nAfter reviewing the output at: ${VAGRANT_DIR}/repose-curl.out\n"
echo -e "Remove the directory at:       ${VAGRANT_DIR}\n\n"
popd
