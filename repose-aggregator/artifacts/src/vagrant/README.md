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
#gradle clean # Optional
./repose-aggregator/artifacts/build-system-packages.sh
```
### --------------------------------------------------------------------------------
```
cd ./repose-aggregator/artifacts/build/Vagrant/deb
```
*OR*
```
cd ./repose-aggregator/artifacts/build/Vagrant/rpm
```
### --------------------------------------------------------------------------------
```
vagrant destroy
```
### --------------------------------------------------------------------------------
###### NOTE: Since a version other than the latest published artifacts is to be used, edit the top of the `Vagrantfile`.
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
###### NOTE: If a version other than the latest published artifacts is to be used, then edit the top of the `Vagrantfile`.
### --------------------------------------------------------------------------------
```
mkdir -p ./etc_repose &&
cp ../test/common/* \
   ./etc_repose/ &&
cp ../test/REP-4077_Verify7-3-6-0/* \
   ./etc_repose/ &&
vagrant destroy &&
vagrant up &&
sleep 1 &&
vagrant ssh
```
###### NOTE: The `sleep` is only needed if scripting this sequence in order to give the VM enough time to start listening on the port.
