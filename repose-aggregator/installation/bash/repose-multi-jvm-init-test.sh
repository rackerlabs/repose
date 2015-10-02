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
START=$(date +"%s")
echo -en "Starting at: $(date)\n"

export WDIR=`mktemp -d`
echo "WORKING IN ${WDIR}"

SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
#export REPOSE_DIR=${SCRIPT_DIR}/../../../
export VAGRANT_DIR=${WDIR}/Vagrant
export PATCH_DIR=${SCRIPT_DIR}
#cd ${REPOSE_DIR}/
#mvn clean install -DskipTests=true
cd ${SCRIPT_DIR}
#mvn clean install -P build-system-packages
mkdir -p ${VAGRANT_DIR}

# clean out the vagrant directory
cd ${VAGRANT_DIR}
rm -f Vagrantfile repose-*.noarch.rpm
# copy in the RPMs
cp -f ${SCRIPT_DIR}/../rpm/repose-*/target/rpm/*/RPMS/noarch/repose-*.noarch.rpm ${VAGRANT_DIR}/
# copy in the patch for starting up repose
cp -f ${PATCH_DIR}/repose-test_system-model.cfg.patch ${VAGRANT_DIR}/

# barf out the logic we want to run in centos to test multiple JVMs
cat << 'EOF' > ${VAGRANT_DIR}/validation.sh
#!/bin/bash

echo "Installing prerequisites..."
sudo yum  install -y wget curl patch python-setuptools
sudo wget http://apt.sw.be/redhat/el6/en/x86_64/rpmforge/RPMS/rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo rpm -Uvh rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo yum install -y daemonize
sudo yum --nogpgcheck localinstall -y /vagrant/repose-valve-*.rpm /vagrant/repose-filter-bundle-*.rpm /vagrant/repose-extensions-filter-bundle-*.rpm
sudo easy_install pip
echo "DONE"

echo "Applying patch to the system model to get it to fire up"
sudo cp -f /etc/repose/system-model.cfg.xml /vagrant/system-model.cfg.xml_ORIG
sudo patch /etc/repose/system-model.cfg.xml < /vagrant/repose-test_system-model.cfg.patch

echo "First test should fail to start up, because default java is 1.6.0"
sudo /etc/init.d/repose-valve start
export RESULT=$?

echo "===================================="
echo "RESULT OF STARTING REPOSE IS: ${RESULT}"
echo "===================================="

if [[ "$RESULT" == "0" ]]; then
 echo "FAILURE, REPOSE SHOULD NOT PRETEND TO START"
 exit 1
fi

sudo yum remove -y java-1.6.0-openjdk.x86_64
sudo yum install -y java-1.7.0-openjdk.x86_64
sudo yum --nogpgcheck localinstall -y /vagrant/repose-valve-*.rpm /vagrant/repose-filter-bundle-*.rpm /vagrant/repose-extensions-filter-bundle-*.rpm

echo "Repose should start nicely on jdk 1.7.0"
sudo /etc/init.d/repose-valve start
RESULT=$?

if [[ "$RESULT" == "1" ]]; then
 echo "FAILURE, REPOSE SHOULD HAVE STARTED ON 1.7.0"
 exit 1
fi
sudo /etc/init.d/repose-valve stop

sudo yum remove -y java-1.7.0-openjdk.x86_64
sudo yum install -y java-1.8.0-openjdk.x86_64
sudo yum --nogpgcheck localinstall -y /vagrant/repose-valve-*.rpm /vagrant/repose-filter-bundle-*.rpm /vagrant/repose-extensions-filter-bundle-*.rpm

echo "Repose should start nicely on jdk 1.8.0"
sudo /etc/init.d/repose-valve start
RESULT=$?

if [[ "$RESULT" == "1" ]]; then
 echo "FAILURE, REPOSE SHOULD HAVE STARTED ON 1.8.0"
 exit 1
fi
sudo /etc/init.d/repose-valve stop


EOF

vagrant init centos-65-x64-virtualbox-puppet http://puppet-vagrant-boxes.puppetlabs.com/centos-65-x64-virtualbox-puppet.box
vagrant up

vagrant ssh -c "/bin/bash /vagrant/validation.sh"
STATUS=$?
if [ $STATUS -eq 0 ]; then
    vagrant destroy -f
    echo -e "\n\nReview the test output at: ${VAGRANT_DIR}/repose-curl.out\n"
else
    echo -e "\n\nDid not destroy the VM since there was an error.\n"
    echo -e "After reviewing the state of the VM at: ${VAGRANT_DIR}\n"
    echo -e "Destroy it and remove the directory:    vagrant destroy -f\n\n"
fi

STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTotal time: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n"
echo -en "Finished at: $(date)\n"
exit $STATUS
