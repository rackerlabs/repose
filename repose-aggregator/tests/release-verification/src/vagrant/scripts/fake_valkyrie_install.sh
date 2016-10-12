echo "-------------------------------------------------------------------------------------------------------------------"
echo "Installing Fake Valkyrie"
echo "-------------------------------------------------------------------------------------------------------------------"

cd /opt/fake-valkyrie
echo `pwd`

# download and install the Fake Valkyrie app dependencies
npm install

sudo chmod 755 /opt/fake-valkyrie
