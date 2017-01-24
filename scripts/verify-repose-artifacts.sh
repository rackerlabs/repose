#!/bin/bash
START=$(date +"%s")
echo -en "Starting at: $(date)\n"
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
#This script will determine the Repose versions in the repositories and process each one.

#This script has only been tested on Ubuntu, and will likely require modification to be used
#on any other Linux distribution. Namely, it is assumed that the deb command is available
#by default but the rpm command requires a package to be installed.

#Constants
## Nexus repository hostname and protocol.
#MVN_HOST='https://maven.research.rackspacecloud.com'
## Nexus search path/prefix.
#MVN_SEARCH="${MVN_HOST}/service/local/lucene/search"
## Nexus Groups to search.
#PACKAGES=("com.rackspace.papi.core" "org.openrepose" )
#End of constants

##Start of script
#echo 'Updating and upgrading packages...' &&
#apt-get update &&
#apt-get upgrade -y &&
#
#echo 'Installing git...' &&
#apt-get install -y git &&

#echo 'Adding Repose debian repository...' &&
#wget -O - http://repo.openrepose.org/debian/pubkey.gpg | sudo apt-key add - &&
#echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list &&
#apt-get update &&

#echo 'Querying repository for Repose artifact versions...' &&
#apt-cache show repose-valve | grep 'Version' | sed 's/Version: //g' | sort &&

#echo 'Querying Nexus for Repose artifact versions...'
## This was used to retrieve the list used below.
#VERSIONS=( "" )
#for package in ${PACKAGES[*]} ; do
#	VERSIONS=(${VERSIONS[*]} `curl -s "${MVN_SEARCH}?r=releases&g=${package}&a=valve&p=jar" |
#	xmllint --xpath '//version' - |
#	sed 's/<version>//g' |
#	sed $'s/<\/version>/\\\n/g' |
#	grep -v '^$' |
#	sort`)
#done
#echo "Versions:"
#for version in ${VERSIONS[*]} ; do
#	echo "   ${version}"
#done

## This was the list used for initial testing.
#VERSIONS=(
#   1.0.1
#   2.6.0
#   6.0.0
#   7.0.1.1
#)

#Loop through all versions within range (2.6.0-7.1.0.2)
VERSIONS=(
#   1.0.1
#   1.0.2
#   1.0.3
#   1.0.4
#   1.0.5
#   1.0.6
#   1.1.0
#   1.1.1
#   1.1.2
#   1.2.0
#   1.2.1
#   1.3.0
#   1.4.0
#   1.4.1
#   1.4.2
#   1.4.3
#   1.4.4
#   1.4.5
#   2.0.0
#   2.1.0
#   2.1.1
#   2.1.2
#   2.1.3
#   2.1.4
#   2.1.5
#   2.1.6
#   2.1.7
#   2.2.0
#   2.2.1
#   2.2.2
#   2.3.0
#   2.3.1
#   2.3.2
#   2.3.3
#   2.3.4
#   2.3.5
#   2.3.6
#   2.4.0
#   2.4.1
#   2.5.0
   2.6.0
   2.6.1
   2.6.2
   2.6.3
   2.6.4
   2.6.5
   2.6.6
   2.6.7
   2.6.8
   2.6.9
   2.6.10
   2.6.11
   2.6.12
   2.7.0
   2.8.0
   2.8.0.2
   2.8.1
   2.8.2
   2.8.3
   2.8.4
   2.8.5
   2.8.6
   2.9.0
   2.10.0
   2.10.1
   2.10.2
   2.11.0
   2.12
   2.12.1
   2.12.2
   2.13.0
   2.13.1
   2.13.2
   3.0.0
   3.0.1
   3.0.2
   3.0.4
   3.0.5
   3.0.6
   3.1.0
   3.1.1
   4.0.0
   4.1.0
   4.1.1
   4.1.2
   4.1.3
   4.1.4
   4.1.5
   5.0.0
   5.0.1
   5.0.2
   5.0.4
   5.0.5
   5.0.9
   6.0.0
   6.0.1
   6.0.2
   6.1.0.3
   6.1.1.0
   6.1.1.1
   6.2.0.0
   6.2.0.1
   6.2.0.2
   6.2.0.3
   6.2.1.0
   6.2.2.0
   7.0.0.0
   7.0.0.1
   7.0.1.0
   7.0.1.1
   7.1.0.0
   7.1.0.1
   7.1.0.2
)
echo "Versions:"
for version in ${VERSIONS[*]} ; do
	echo "   ${version}"
done

for version in ${VERSIONS[*]} ; do
	${SCRIPT_DIR}/verify-repose-artifact.sh ${version}
done # version in ${VERSIONS[*]}
STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTotal time: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n"
echo -en "Finished at: $(date)\n"
#End of script
