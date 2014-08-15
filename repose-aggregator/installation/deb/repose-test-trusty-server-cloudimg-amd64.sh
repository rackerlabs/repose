#!/bin/bash

pushd `pwd`
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
#export REPOSE_DIR=${SCRIPT_DIR}/../../../
export VAGRANT_DIR=${SCRIPT_DIR}/Vagrant
export PATCH_DIR=${SCRIPT_DIR}/../bash
#cd ${REPOSE_DIR}/
#mvn clean install -DskipTests=true
#cd ${REPOSE_DIR}/repose-aggregator/installation/deb
mvn clean install -P build-system-packages
mkdir -p ${VAGRANT_DIR}
cd ${VAGRANT_DIR}
rm -f Vagrantfile repose-*.deb
cp -f ${SCRIPT_DIR}/repose-*/target/repose-*.deb                                 ${VAGRANT_DIR}/
cp -f ${PATCH_DIR}/repose-test_system-model.cfg.patch ${VAGRANT_DIR}/
vagrant init trusty-server-cloudimg-amd64    http://cloud-images.ubuntu.com/vagrant/trusty/current/trusty-server-cloudimg-amd64-vagrant-disk1.box
vagrant up
script='#!/bin/bash\n
sudo apt-get update\n
sudo apt-get install -y wget curl patch python-pip\n
\n
\n
\n
sudo dpkg -i                          /vagrant/repose-valve-*.deb /vagrant/repose-filter-bundle-*.deb /vagrant/repose-extensions-filter-bundle-*.deb\n
sudo apt-get -f install -y\n
sudo cp -f /etc/repose/system-model.cfg.xml /vagrant/system-model.cfg.xml_ORIG\n
sudo patch /etc/repose/system-model.cfg.xml < /vagrant/repose-test_system-model.cfg.patch\n
\n
sudo pip install gunicorn\n
sudo pip install httpbin\n
sudo gunicorn httpbin:app &\n
sudo rm -f /var/log/repose/*.log\n
sudo service repose-valve start\n
echo -en "\\nWaiting for Repose to be ready ..."\n
READY=0\n
while [ $READY -eq 0 ]; do\n
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1\n
   if [ "$?" -eq 0 ]\n
   then\n
      READY=1\n
   else\n
      echo -n " ."\n
      sleep 1\n
   fi\n
done\n
echo -e "\\n\\nRepose is ready."\n
rm -f /vagrant/repose-curl.out\n
for i in {1..11} ; do\n
  echo -e "\\n\\n~~~~~ Attempt #$i ~~~~~\\n\\n" >> /vagrant/repose-curl.out\n
  curl -H "x-pp-user: abc123" -H "Content-Type: Test" -H "Content-Length: 0" localhost:8080/get -v >> /vagrant/repose-curl.out 2>&1\n
done\n
sudo cp -f /var/log/repose/current.log /vagrant/\n
sudo shutdown -h now\n
'
rm -f repose-test.sh
echo -e ${script} > repose-test.sh
chmod a+x repose-test.sh

vagrant ssh -c "/vagrant/repose-test.sh"
vagrant destroy -f
echo -e "\n\nAfter reviewing the output at: ${VAGRANT_DIR}/repose-curl.out\n"
echo -e "Remove the directory at:       ${VAGRANT_DIR}\n\n"
popd
