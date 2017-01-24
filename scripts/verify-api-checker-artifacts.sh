#!/bin/bash
#This script will download and build artifacts for the api-checker library, extract them, decompile all class files, and compare the results.

#Constants
NEXUS_HOST='https://maven.research.rackspacecloud.com' #Nexus repository hostname and protocol
#End of constants

#Start of script
echo 'Updating and upgrading packages...' &&
apt-get update &&
apt-get upgrade &&

echo 'Installing git...' &&
apt-get install -y git &&

echo 'Installing java7...' &&
apt-get install -y openjdk-7-jdk &&
update-alternatives --set java '/usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java' &&
update-alternatives --set javac '/usr/lib/jvm/java-7-openjdk-amd64/bin/javac' &&
export JAVA_HOME=$(readlink -f /usr/bin/javac | sed "s:/bin/javac::") &&

echo 'Installing maven...' &&
apt-get install -y maven &&

echo 'Installing jd-cmd...' &&
rm -rf jd-cmd &&
git clone 'git://github.com/kwart/jd-cmd.git' &&
cd jd-cmd &&
mvn clean package &&
mkdir -p '/opt/jd-cmd/bin' &&
cp 'jd-cli/target/jd-cli.jar' '/opt/jd-cmd/bin/' &&
cd .. &&

echo 'Creating working directories...' &&
rm -rf workspace/api-checker/built &&
rm -rf workspace/api-checker/published &&
mkdir -p workspace/api-checker/built &&
mkdir -p workspace/api-checker/published &&

echo 'Cloning api-checker project...' &&
rm -rf api-checker &&
git clone 'https://github.com/rackerlabs/api-checker.git' &&

for i in $( seq 13 22 ); do
	echo "Downloading api-checker-1.0.$i jar..." &&
	curl -o "workspace/api-checker/published/1.0.$i/checker-core-1.0.$i.jar" --create-dirs \
		"$NEXUS_HOST/service/local/artifact/maven/content?r=releases&g=com.rackspace.papi.components.api-checker&a=checker-core&v=1.0.$i" &&

	echo "Building api-checker-1.0.$i jar..." &&
	cd api-checker/core/ &&
	git checkout "api-checker-1.0.$i" &&
	mvn -DskipTests clean package &&
	cd ../.. &&

	echo "Extracting JAR files for version 1.0.$i..." &&
	mkdir -p "workspace/api-checker/built/1.0.$i/src" &&
	mkdir -p "workspace/api-checker/published/1.0.$i/src" &&
	mv -u "api-checker/core/target/checker-core-1.0.$i.jar" "workspace/api-checker/built/1.0.$i/checker-core-1.0.$i.jar" &&

	echo "Decompiling class files for version 1.0.$i..." &&
	java -jar /opt/jd-cmd/bin/jd-cli.jar -od "workspace/api-checker/built/1.0.$i/src/" "workspace/api-checker/built/1.0.$i/checker-core-1.0.$i.jar" &&
	java -jar /opt/jd-cmd/bin/jd-cli.jar -od "workspace/api-checker/published/1.0.$i/src/" "workspace/api-checker/published/1.0.$i/checker-core-1.0.$i.jar" &&

	echo "Comparing files for version 1.0.$i..." &&
	find "workspace/api-checker/built/1.0.$i/src/" -type f -exec md5sum {} + | sort -k 2 > "workspace/api-checker/built/1.0.$i/built.md5" &&
	sed -i 's/built\///g' "workspace/api-checker/built/1.0.$i/built.md5" &&
	find "workspace/api-checker/published/1.0.$i/src/" -type f -exec md5sum {} + | sort -k 2 > "workspace/api-checker/published/1.0.$i/published.md5" &&
	sed -i 's/published\///g' "workspace/api-checker/published/1.0.$i/published.md5" &&
	diff "workspace/api-checker/built/1.0.$i/built.md5" "workspace/api-checker/published/1.0.$i/published.md5" > "workspace/api-checker/report-1.0.$i.diff" &&

	echo ''
done

echo 'Done'
#End of script
