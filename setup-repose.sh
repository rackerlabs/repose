#!/bin/bash

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

  for f in $DIR/repose-aggregator/core/valve/target/repose-valve.jar
  do
    echo ln -s $f "$1/usr/share/repose/"
    ln -s $f "$1/usr/share/repose/"
  done

fi

