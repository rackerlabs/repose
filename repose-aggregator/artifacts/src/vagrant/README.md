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



# Build the Repose artifacts locally then install and run them in a Vagrant VM.
```
cd ~/Projects/repose
#gradle clean # Optional
~/Projects/repose/repose-aggregator/artifacts/build-system-packages.sh
```
### --------------------------------------------------------------------------------
```
cd ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/deb
```
*OR*
```
cd ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/rpm
```
### --------------------------------------------------------------------------------
```
vagrant destroy
vagrant --repose-version='local' up
```
###### NOTE: The following is only needed if scripting this sequence in order to give the VM enough time to start listening on the port.
```
sleep 1
vagrant ssh
```



# Install and run the latest Repose artifacts OR a specific version in a Vagrant VM.
```
mkdir -p ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/
cp -R ~/Projects/repose/repose-aggregator/artifacts/src/vagrant/* \
      ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/
```
### --------------------------------------------------------------------------------
```
cd ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/deb
```
*OR*
```
cd ~/Projects/repose/repose-aggregator/artifacts/build/Vagrant/rpm
```
### --------------------------------------------------------------------------------
```
vagrant destroy
```
### --------------------------------------------------------------------------------
```
vagrant up
```
*OR*
```
vagrant --repose-version='8.0.0.0' up
```
### --------------------------------------------------------------------------------
###### NOTE: The following is only needed if scripting this sequence in order to give the VM enough time to start listening on the port.
```
sleep 1
vagrant ssh
```
