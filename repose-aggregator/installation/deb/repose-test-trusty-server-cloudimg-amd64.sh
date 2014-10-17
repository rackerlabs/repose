#!/bin/bash

pushd `pwd`
# This is where this script is executing from.
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
#export REPOSE_DIR=${SCRIPT_DIR}/../../../
export VAGRANT_DIR=${SCRIPT_DIR}/Vagrant
export PATCH_DIR=${SCRIPT_DIR}/../bash
## This is only if you want to clean and build the entire project heirarchy.
#cd ${REPOSE_DIR}/
#mvn clean install -DskipTests=true
#cd ${REPOSE_DIR}/repose-aggregator/installation/deb
# Build the installer packages based on the currently install artifacts.
mvn clean install -P build-system-packages
# IF the build failed, THEN exit.
if [ "$?" -ne "0" ]
then
  exit $?
fi
# Set up the Vagrant directory and add all the local files so the VM has access to them.
mkdir -p ${VAGRANT_DIR}
cd ${VAGRANT_DIR}
rm -f Vagrantfile repose-*.deb
cp -f ${SCRIPT_DIR}/repose-*/target/repose-*.deb                                 ${VAGRANT_DIR}/
cp -f ${PATCH_DIR}/repose-test_system-model.cfg.patch ${VAGRANT_DIR}/
# Instantiate and start the VM.
vagrant init trusty-server-cloudimg-amd64    http://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box
vagrant up
# This is the script that is ran inside the VM.
script='#!/bin/bash\n
# Insure the required packages are installed.\n
sudo apt-get update\n
sudo apt-get install -y wget curl patch python-pip\n
\n
\n
\n
# Install the Repose packages.\n
sudo dpkg -i                          /vagrant/repose-war-*.deb /vagrant/repose-valve-*.deb /vagrant/repose-filters-*.deb /vagrant/repose-extension-filters-*.deb\n
sudo apt-get -f install -y\n
# Modify the default system-model to add a header-translation filter and change the endpoint to the local GUnicorn instance.\n
sudo cp -f /etc/repose/system-model.cfg.xml /vagrant/system-model.cfg.xml_ORIG\n
sudo patch /etc/repose/system-model.cfg.xml < /vagrant/repose-test_system-model.cfg.patch\n
# Install and start the GUnicorn instance.\n
\n
sudo pip install gunicorn\n
sudo pip install httpbin\n
sudo gunicorn httpbin:app &\n
# Allocate swap space\n
sudo dd if=/dev/zero of=/swapfile bs=1024k count=1024\n
sudo mkswap /swapfile\n
sudo swapon /swapfile\n
# Clean up any previous execution and start Repose.\n
sudo rm -f /var/log/repose/*.log\n
sudo service repose-valve start\n
echo -en "\\nWaiting for Repose to be ready ..."\n
READY=0\n
COUNT=0\n
TIMEOUT=30\n
while [ $READY -eq 0 ]; do\n
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1\n
   if [ "$?" -eq 0 ]\n
   then\n
      READY=1\n
   else\n
      let "COUNT +=1"\n
      if [ "$COUNT" -ge "$TIMEOUT" ]\n
      then\n
         echo -e "\\n\\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\\n\\n"\n
         exit 5\n
      fi\n
      echo -n " ."\n
      sleep 1\n
   fi\n
done\n
echo -e "\\n\\nRepose is ready."\n
# Clean up any previous test output and test Repose.\n
rm -f /vagrant/repose-curl.out\n
for i in {1..11} ; do\n
  echo -e "\\n\\n~~~~~ Attempt #$i ~~~~~\\n\\n" >> /vagrant/repose-curl.out\n
  curl -H "x-pp-user: abc123" -H "Content-Type: Test" -H "Content-Length: 0" localhost:8080/get -v >> /vagrant/repose-curl.out 2>&1\n
done\n
# Copy the Repose logs so they are accessible after the VM is destroyed.\n
sudo cp -f /var/log/repose/current.log /vagrant/\n
sudo shutdown -h now\n
'
# Remove any previous version of the test script and create a new one with the appropriate execute permissions.
rm -f repose-test.sh
echo -e ${script} > repose-test.sh
chmod a+x repose-test.sh

# Run the test and destroy the VM.
vagrant ssh -c "/vagrant/repose-test.sh"
vagrant destroy -f
# Let the user know where the out put is located.
echo -e "\n\nAfter reviewing the output at: ${VAGRANT_DIR}/repose-curl.out\n"
echo -e "Remove the directory at:       ${VAGRANT_DIR}\n\n"
popd
