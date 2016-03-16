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

FILES=(\
#/etc \
#/etc/init.d \
/etc/init.d/repose-valve \
#/etc/logrotate.d \
/etc/logrotate.d/repose \
/etc/repose \
/etc/repose/client-auth-n.cfg.xml \
/etc/repose/container.cfg.xml \
/etc/repose/destination-router.cfg.xml \
#/etc/repose/dist-datastore.cfg.xml \
/etc/repose/fail-404.wadl \
/etc/repose/header-user.cfg.xml \
/etc/repose/header-id-mapping.cfg.xml \
/etc/repose/header-normalization.cfg.xml \
/etc/repose/header-translation.cfg.xml \
/etc/repose/ip-user.cfg.xml \
/etc/repose/log4j2.xml \
/etc/repose/openstack-authorization.cfg.xml \
/etc/repose/pass.wadl \
/etc/repose/rate-limiting.cfg.xml \
/etc/repose/repose-valve.conf \
/etc/repose/response-messaging.cfg.xml \
/etc/repose/system-model.cfg.xml \
/etc/repose/uri-identity.cfg.xml \
/etc/repose/uri-normalization.cfg.xml \
/etc/repose/uri-stripper.cfg.xml \
/etc/repose/validator.cfg.xml \
/etc/repose/versioning.cfg.xml \
#/usr \
#/usr/bin \
/usr/bin/clean-repose-deploy \
#/usr/share \
#/usr/share/doc \
#/usr/share/doc/repose-extensions-filter-bundle \
/usr/share/doc/repose-extensions-filter-bundle/LICENSE.txt \
#/usr/share/doc/repose-filter-bundle \
/usr/share/doc/repose-filter-bundle/LICENSE.txt \
#/usr/share/doc/repose-valve \
/usr/share/doc/repose-valve/LICENSE.txt \
#/usr/share/doc/repose-war \
/usr/share/doc/repose-war/LICENSE.txt \
/usr/share/repose \
/usr/share/repose/filters \
/usr/share/repose/filters/extensions-filter-bundle-7.0.0.0-SNAPSHOT.ear \
/usr/share/repose/filters/filter-bundle-7.0.0.0-SNAPSHOT.ear \
/usr/share/repose/repose-7.0.0.0-SNAPSHOT.war \
/usr/share/repose/repose-valve.jar \
#/var \
#/var/log \
/var/log/repose \
/var/repose \
)

for file in ${FILES[*]} ; do
   if [ -d $file ]; then
      ls -ld $file
   elif [ -f $file ]; then
      ls -l $file
   else
      echo "----------  MISSING MISSING     $file"
   fi
done \
| cut -d' ' -f1,3,4,9 \
| sed 's/root root/  root \/   root/g' \
| sed 's/repose repose/repose \/ repose/g' #> /vagrant/Files.out
