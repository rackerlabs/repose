echo "-------------------------------------------------------------------------------------------------------------------"
echo "Installing Fake Origin"
echo "-------------------------------------------------------------------------------------------------------------------"

cd /opt/fake-origin
echo `pwd`

# download and install the Fake Origin app dependencies
npm install

sudo chmod 755 /opt/fake-origin
