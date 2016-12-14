#!/bin/bash
echo "-------------------------------------------------------------------------------------------------------------------"
echo "Installing Unmanaged NodeJS"
echo "-------------------------------------------------------------------------------------------------------------------"

# Download and Unzip the current LTS version to a temporary location.
cd ~/tmp/
wget -q https://nodejs.org/dist/v6.9.1/node-v6.9.1-linux-x64.tar.gz
tar -xzf node-v6.9.1-linux-x64.tar.gz

# Move it to the correct place.
rm -f /usr/local/lib/node-current /usr/local/lib/node-v6.9.1-linux-x64
mv node-v6.9.1-linux-x64 /usr/local/lib/

# Add in the links that would have normally been handled by the package manager.
cd /usr/local/lib/
ln -s node-v6.9.1-linux-x64 node-current
cd /usr/local/bin/
rm -f node
rm -f npm
ln -s ../lib/node-current/bin/node
ln -s ../lib/node-current/bin/npm
