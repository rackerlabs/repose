echo "-------------------------------------------------------------------------------------------------------------------"
echo "Starting Fake Origin"
echo "-------------------------------------------------------------------------------------------------------------------"

cd /opt/fake-origin
node app.js 2>&1 >> /vagrant/fake-origin.log &
sleep 2

echo "-------------------------------------------------------------------------------------------------------------------"
echo "Testing Fake Origin"
echo "-------------------------------------------------------------------------------------------------------------------"
curl -v http://127.0.0.1:8000
echo ""
