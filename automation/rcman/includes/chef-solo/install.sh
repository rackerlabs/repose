#!/bin/bash
 
# This runs as root on the server
 
chef_binary=/var/lib/gems/1.9.1/bin/chef-solo
 
# Are we on a vanilla system?
if ! test -f "$chef_binary"; then
    export DEBIAN_FRONTEND=noninteractive
    # Upgrade headlessly (this is only safe-ish on vanilla systems)
    aptitude update &&
    apt-get -o Dpkg::Options::="--force-confnew" \
        --force-yes -fuy dist-upgrade &&
    # Install Ruby and Chef
    aptitude install -y openjdk-6-jdk ruby1.9.1 ruby1.9.1-dev make rubygems1.9.1 libopenssl-ruby1.9.1 &&
    sudo gem1.9.1 install --no-rdoc --no-ri chef --version 10.18.2
fi 
ln -s /var/lib/gems/1.9.1/bin/chef-solo /usr/bin/chef-solo 
ln -s /usr/bin/ruby1.9.1 /usr/bin/ruby
