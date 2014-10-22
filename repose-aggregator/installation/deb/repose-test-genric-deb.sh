#!/bin/bash
sudo apt-get update
sudo apt-get install -y wget curl patch python-pip



sudo dpkg -i                          /vagrant/repose-valve-*.deb /vagrant/repose-filter-bundle-*.deb /vagrant/repose-extensions-filter-bundle-*.deb
sudo apt-get -f install -y
