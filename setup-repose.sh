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

if [ -z "$1" ] ; then
  echo "Usage: $0 dest-folder"
else

  DIR=`dirname $0`

  mvn -f "$DIR/pom.xml" -Dmaven.test.skip=true -DskipTests install

  mkdir -p "$1/usr/share/repose/filters"
  mkdir -p "$1/var/repose"
  mkdir -p "$1/var/log/repose"
  mkdir -p "$1/etc/repose"

  for f in "$1"/usr/share/repose/* "$1"/usr/share/repose/filters/*
  do
    if [ -f $f ]; then
      echo unlink $f
      unlink $f
    fi
  done

  for f in $DIR/repose-aggregator/components/filter-bundle/target/filter-bundle*.ear $DIR/repose-aggregator/extensions/extensions-filter-bundle/target/extensions-filter-bundle*.ear
  do
    echo ln -s $f "$1/usr/share/repose/filters/"
    ln -s $f "$1/usr/share/repose/filters/"
  done

  for f in $DIR/repose-aggregator/core/valve/target/repose.jar
  do
    echo ln -s $f "$1/usr/share/repose/"
    ln -s $f "$1/usr/share/repose/"
  done

fi

