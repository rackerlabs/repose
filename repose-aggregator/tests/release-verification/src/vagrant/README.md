# Required Technologies:
* Git
* Vagrant
* VirtualBox
* Gradle (only for from source installs) 



# Clone the Repose source code repository.
```
mkdir -p ~/Projects
cd ~/Projects
```
### --------------------------------------------------------------------------------
```
git clone https://github.com/rackerlabs/repose.git # HTTPS
```
*OR*
```
git clone git@github.com:rackerlabs/repose.git     # SSH
```
### --------------------------------------------------------------------------------
```
cd ~/Projects/repose
```



# Build the Repose artifacts locally then install and run them in a Vagrant VM.
```
# Optionaly
#gradle clean &&
./repose-aggregator/artifacts/build-system-packages.sh &&
```
### --------------------------------------------------------------------------------
```
cd ./repose-aggregator/artifacts/build/Vagrant/deb &&
```
*OR*
```
cd ./repose-aggregator/artifacts/build/Vagrant/rpm &&
```
### --------------------------------------------------------------------------------
```
vagrant destroy &&
```
### --------------------------------------------------------------------------------
###### NOTE: Since a version other than the latest published artifacts is to be used, export the following environment variable.
```
export REPOSE_VERSION=local &&
```
### --------------------------------------------------------------------------------
```
vagrant up &&
sleep 1 &&
vagrant ssh
```
###### NOTE: The `sleep` is only needed if scripting this sequence in order to give the VM enough time to start listening on the port.



# Install and run the latest published Repose artifacts OR a specific version in a Vagrant VM.
```
mkdir -p ./repose-aggregator/artifacts/build/Vagrant/ &&
cd ./repose-aggregator/artifacts/build/Vagrant/ &&
cp -R ../../src/vagrant/* \
      ./ &&
```
### --------------------------------------------------------------------------------
```
cd ./deb &&
```
*OR*
```
cd ./rpm &&
```
### --------------------------------------------------------------------------------
```
vagrant destroy --force &&
```
### --------------------------------------------------------------------------------
###### NOTE: This environment variable only needs configured if a version other than the latest published artifacts is to be used.
```
export REPOSE_VERSION=7.3.8.0 &&
```
*OR*
```
export REPOSE_VERSION=local &&
```
### --------------------------------------------------------------------------------
```
vagrant up &&
sleep 1 &&
vagrant ssh
```
###### NOTE: The `sleep` is only needed if scripting this sequence in order to give the VM enough time to start listening on the port.

# Execute a release validation test.
After executing the steps above to prepare a VM and instead of the `sleep` or `vagrant ssh`,
execute the following commands to make a request against the default Repose installation.
It should result in a *Moved Permanently* (`301`) status.
```
vagrant ssh -c 'echo -en "\nWaiting for Repose to be ready ..."
READY=0
COUNT=0
TIMEOUT=30
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /var/log/repose/current.log >> /dev/null 2>&1
   if [ "$?" -eq 0 ]; then
      READY=1
   else
      let "COUNT +=1"
      if [ "$COUNT" -ge "$TIMEOUT" ]; then
         break
      fi
      echo -n " ."
      sleep 1
   fi
done
if [ $READY -eq 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
else
   echo -en "\n\nRepose is ready.\n"
   curl -v http://localhost:8080 > /vagrant/default.log 2>&1
fi
'
```

Now copy the test files within scope of the VM and install them in the running Repose instance.
It should result in an *Ok* (`200`) status.
```
mkdir -p ./etc_repose &&
cp ../test/common/* \
   ./etc_repose/ &&
cp ../test/REP-4077_Verify7-3-6-0/* \
   ./etc_repose/ &&
vagrant ssh -c 'sudo sh -c "echo TRUNCATED > /vagrant/var-log-repose-current.log"
sudo mkdir -p /etc/repose/orig
sudo sh -c "cp /etc/repose/*.* /etc/repose/orig/"
sudo cp /vagrant/etc_repose/*.* /etc/repose/
echo -en "\nWaiting for Repose to be ready ..."
READY=0
COUNT=0
TIMEOUT=30
while [ $READY -eq 0 ]; do
   sudo grep "Repose ready" /vagrant/var-log-repose-current.log >> /dev/null 2>&1
   if [ "$?" -eq 0 ]; then
      READY=1
   else
      let "COUNT +=1"
      if [ "$COUNT" -ge "$TIMEOUT" ]; then
         break
      fi
      echo -n " ."
      sleep 1
   fi
done
if [ $READY -eq 0 ]; then
   echo -en "\n\n~~~~~ ERROR - REPOSE FAILED TO START - VM Left Running ~~~~~\n\n"
   exit 199
else
   echo -en "\n\nRepose is ready.\n"
   curl -v http://localhost:8080/resource/this-is-an-id > /vagrant/validation.log 2>&1
fi
'
```
