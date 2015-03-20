#!/bin/bash

###
# #%L
# Repose
# %%
# Copyright (C) 2010 - 2015 Rackspace US, Inc.
# %%
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
# #L%
###

sudo yum     install -y wget curl patch python-setuptools
sudo wget http://apt.sw.be/redhat/el6/en/x86_64/rpmforge/RPMS/rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo rpm -Uvh rpmforge-release-0.5.2-2.el6.rf.x86_64.rpm
sudo yum install -y daemonize
sudo yum --nogpgcheck localinstall -y /vagrant/repose-valve-*.rpm /vagrant/repose-filter-bundle-*.rpm /vagrant/repose-extensions-filter-bundle-*.rpm
sudo easy_install pip
