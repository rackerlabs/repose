#!/bin/bash

sudo yum     install -y wget curl patch python-setuptools
sudo wget http://apt.sw.be/redhat/el6/en/x86_64/rpmforge/RPMS/rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo rpm -Uvh rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo yum install -y daemonize
sudo yum --nogpgcheck localinstall -y /vagrant/repose-valve-*.rpm /vagrant/repose-filters-*.rpm       /vagrant/repose-extension-filters-*.rpm
sudo easy_install pip
