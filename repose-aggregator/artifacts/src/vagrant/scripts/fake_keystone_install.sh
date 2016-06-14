echo "-------------------------------------------------------------------------------------------------------------------"
echo "Installing Fake Keystone"
echo "-------------------------------------------------------------------------------------------------------------------"

cd /opt/fake-keystone
echo `pwd`

# download and install the Fake Keystone app dependencies
npm install

sudo chmod 755 /opt/fake-keystone
