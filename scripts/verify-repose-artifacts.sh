#!/bin/bash
#This script will download and build artifacts for the Repose project, and compare the results.

#This script has only been tested on Ubuntu, and will likely require modification to be used
#on any other Linux distribution. Namely, it is assumed that the deb command is available
#by default but the rpm command requires a package to be installed.

#Constants
NEXUS_HOST='https://maven.research.rackspacecloud.com' #Nexus repository hostname and protocol
#End of constants

#Functions
set_java_version() {
	if [ "$1" -ne "6" -a "$1" -ne "7" -a "$1" -ne "8" ]; then
		echo 'Attempted to set invalid or unsupported version of Java'
		exit 1
	fi
	apt-get install -y "openjdk-$1-jdk" &&
	update-alternatives --set java "/usr/lib/jvm/java-$1-openjdk-amd64/jre/bin/java" &&
	update-alternatives --set javac "/usr/lib/jvm/java-$1-openjdk-amd64/bin/javac" &&
	export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")
}
#End of functions

#Start of script
echo 'Updating and upgrading packages...' &&
apt-get update &&
apt-get upgrade &&

echo 'Installing git...' &&
apt-get install -y git &&

echo 'Using java6 first...' &&
set_java_version 6 &&

echo 'Adding Repose debian repository...' &&
wget -O - http://repo.openrepose.org/debian/pubkey.gpg | sudo apt-key add - &&
echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list &&
apt-get update &&

echo 'Querying repository for Repose artifact versions...' &&
apt-cache show repose-valve | grep 'Version' | sed 's/Version: //g' | sort &&


echo 'Querying Nexus for Repose artifact versions...' &&
curl 'https://maven.research.rackspacecloud.com/service/local/lucene/search?repositoryId=releases&g=com.rackspace.papi.core&a=valve&p=jar' |
xmllint --xpath '//version' - | sed 's/<version>//g' | sed $'s/<\/version>/\\\n/g' | grep -v '^$' | sort &&
for v in : do
	
done
#todo: parse artifact names and version from rpm/deb instead?
#todo: fetch all artifacts on Nexus of a specific version
curl 'https://maven.research.rackspacecloud.com/service/local/lucene/search?repositoryId=releases&g=com.rackspace.papi&v=$VERSION&p=jar' #todo: jar, ear, or war
'workspace/repose/repo/rpm/$RPM_VERSION' #rpm working directory
'workspace/repose/repo/deb/$DEB_VERSION' #deb working directory
'workspace/repose/nexus/$NEXUS_VERSION' #nexus working directory

#Loop through all versions within range (2.3-7.0)

#End of script
