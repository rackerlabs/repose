#!/bin/bash
START=$(date +"%s")
echo -en "Starting at: $(date)\n"
#This script will download and compare the provided Repose version's artifacts from the repositories.

#This script has only been tested on Ubuntu, and will likely require modification to be used
#on any other Linux distribution. Namely, it is assumed that the deb command is available
#by default but the rpm command requires a package to be installed.

#Constants
# Repose RPM repository hostname, protocol, and path.
RPM_REPO='http://repo.openrepose.org/el/'
# Repose DEB repository hostname, protocol, and path.
DEB_REPO='http://repo.openrepose.org/debian/pool/stable/main/r'
# Nexus repository hostname and protocol.
MVN_HOST='https://maven.research.rackspacecloud.com'
# Nexus search path/prefix.
MVN_SEARCH="${MVN_HOST}/service/local/lucene/search"
# Nexus content path/prefix.
MVN_CONTENT="${MVN_HOST}/service/local/artifact/maven/content"
# Nexus Groups to search.
PACKAGES=(
	"com.rackspace.papi.commons"
	"com.rackspace.papi.components"
	"com.rackspace.papi.components.extensions"
	"com.rackspace.papi.core"
	"com.rackspace.papi."
	"com.rackspace.papi."
	"com.rackspace.papi."
	"org.openrepose"
)
# Path to workspace directory.
WORKSPACE_DIR='/root/workspace'
#End of constants

#Functions
extract_archives() {
	for archive in `find ${1} -name "*.[jew]ar"`; do
		extract_archive ${archive}
	done
}

extract_archive() {
	if [ ! -d ${1}_extract ]; then
		START_DIR=${PWD} &&
		echo -en "      Extracting ${1} ...\n" &&
		mkdir ${1}_extract &&
		mv ${1} ${1}_extract &&
		cd ${1}_extract &&
		jar xf ${1##*/} &&
		mv ${1##*/} ../ &&
		cd ${START_DIR} &&
		unset START_DIR
	#else
	#	echo -en "      ${1} ALREADY EXTRACTED\n"
	fi
}

decompile_jars() {
	for jar in `find ${1} -name "*.jar"`; do
		decompile_jar ${jar}
	done
}

decompile_jar() {
	if [ ! -d ${1}_decompile ]; then
		echo -en "      Decompiling ${1} ...\n" &&
		java -jar /opt/jd-cmd/bin/jd-cli.jar -g "WARN" -od "${1}_decompile" "${1}"
	#else
	#	echo -en "      ${1} ALREADY DECOMPILED\n"
	fi
}
#End of functions

##Start of script
version=$1
mkdir -p ${WORKSPACE_DIR}/repose/${version}/
echo -en "\n\nProcessing Version v${version}...\n" | tee -a ${WORKSPACE_DIR}/repose/${version}/verify.txt
########################################################################################################################
echo -en "\nRetrieving RPM v${version}...\n"
mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/
cd ${WORKSPACE_DIR}/repose/${version}/rpm/
echo -en "Retrieving RPM WAR v${version}... "
wget -q ${RPM_REPO}/repose-war-${version}-1.noarch.rpm
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	rpm2cpio repose-war-${version}-1.noarch.rpm | cpio -i --make-directories
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/war
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/var/lib/tomcat7/webapps/ROOT.war ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/var/lib/tomcat7/webapps/ROOT.war ${WORKSPACE_DIR}/repose/${version}/rpm/war/web-application-${version}.war
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/ROOT.war ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/ROOT.war ${WORKSPACE_DIR}/repose/${version}/rpm/war/web-application-${version}.war
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/rpm/etc ${WORKSPACE_DIR}/repose/${version}/rpm/usr ${WORKSPACE_DIR}/repose/${version}/rpm/var &&
	extract_archive ${WORKSPACE_DIR}/repose/${version}/rpm/war/web-application-${version}.war &&
	find . -name "*-${version}.jar" -prune -o -name "checker-core-*.jar" -prune -o -name "*.jar" -exec rm {} \;
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/rpm/
echo -en "Retrieving RPM Valve v${version}... "
wget -q ${RPM_REPO}/repose-valve-${version}-1.noarch.rpm
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	rpm2cpio repose-valve-${version}-1.noarch.rpm | cpio -i --make-directories
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/valve
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/repose-valve.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/repose-valve.jar ${WORKSPACE_DIR}/repose/${version}/rpm/valve/valve-${version}.jar
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/lib/repose/repose-valve.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/lib/repose/repose-valve.jar ${WORKSPACE_DIR}/repose/${version}/rpm/valve/valve-${version}.jar
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/rpm/etc ${WORKSPACE_DIR}/repose/${version}/rpm/usr ${WORKSPACE_DIR}/repose/${version}/rpm/var
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/rpm/
echo -en "Retrieving RPM Filters v${version}... "
wget -q ${RPM_REPO}/repose-filters-${version}-1.noarch.rpm || wget -q ${RPM_REPO}/repose-filter-bundle-${version}-1.noarch.rpm
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	rpm2cpio repose-filters-${version}-1.noarch.rpm | cpio -i --make-directories
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/filters &&
	mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/filters/filter-bundle-${version}.ear ${WORKSPACE_DIR}/repose/${version}/rpm/filters/ &&
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/rpm/etc ${WORKSPACE_DIR}/repose/${version}/rpm/usr ${WORKSPACE_DIR}/repose/${version}/rpm/var &&
	extract_archive ${WORKSPACE_DIR}/repose/${version}/rpm/filters/filter-bundle-${version}.ear &&
	find . -name "*-${version}.jar" -prune -o -name "checker-core-*.jar" -prune -o -name "*.jar" -exec rm {} \;
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/rpm/
echo -en "Retrieving RPM Extenions v${version}... "
wget -q ${RPM_REPO}/repose-extension-filters-${version}-1.noarch.rpm || wget -q ${RPM_REPO}/repose-extensions-filter-bundle-${version}-1.noarch.rpm
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	rpm2cpio repose-extension-filters-${version}-1.noarch.rpm | cpio -i --make-directories
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/extensions &&
	mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/filters/extensions-filter-bundle-${version}.ear ${WORKSPACE_DIR}/repose/${version}/rpm/extensions/ &&
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/rpm/etc ${WORKSPACE_DIR}/repose/${version}/rpm/usr ${WORKSPACE_DIR}/repose/${version}/rpm/var &&
	extract_archive ${WORKSPACE_DIR}/repose/${version}/rpm/extensions/extensions-filter-bundle-${version}.ear &&
	find . -name "*-${version}.jar" -prune -o -name "checker-core-*.jar" -prune -o -name "*.jar" -exec rm {} \;
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/rpm/
echo -en "Retrieving RPM CLI Utils v${version}... "
wget -q ${RPM_REPO}/repose-cli-utils-${version}-1.noarch.rpm
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	rpm2cpio repose-cli-utils-${version}-1.noarch.rpm | cpio -i --make-directories
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/rpm/cli-utils &&
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/repose-cli.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/repose/repose-cli.jar ${WORKSPACE_DIR}/repose/${version}/rpm/cli-utils/cli-utils-${version}.jar
	[ -f ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/lib/repose/repose-cli.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/rpm/usr/share/lib/repose/repose-cli.jar ${WORKSPACE_DIR}/repose/${version}/rpm/cli-utils/cli-utils-${version}.jar
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/rpm/etc ${WORKSPACE_DIR}/repose/${version}/rpm/usr ${WORKSPACE_DIR}/repose/${version}/rpm/var
else
	echo -en "FAILED\n"
fi
echo -en "\n\nRetrieving DEB v${version}...\n"
mkdir -p ${WORKSPACE_DIR}/repose/${version}/deb/
cd ${WORKSPACE_DIR}/repose/${version}/deb/
echo -en "Retrieving DEB Valve v${version}... "
wget -q ${DEB_REPO}/repose-valve/repose-valve_${version}_all.deb
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	ar p repose-valve_${version}_all.deb data.tar.gz | tar zx &&
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/deb/valve &&
	[ -f ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/repose-valve.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/repose-valve.jar ${WORKSPACE_DIR}/repose/${version}/deb/valve/valve-${version}.jar
	[ -f ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/lib/repose/repose-valve.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/lib/repose/repose-valve.jar ${WORKSPACE_DIR}/repose/${version}/deb/valve/valve-${version}.jar
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/deb/etc ${WORKSPACE_DIR}/repose/${version}/deb/usr ${WORKSPACE_DIR}/repose/${version}/deb/var
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/deb/
echo -en "Retrieving DEB Filters v${version}... "
wget -q ${DEB_REPO}/repose-filter-bundle/repose-filter-bundle_${version}_all.deb
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	ar p repose-filter-bundle_${version}_all.deb data.tar.gz | tar zx &&
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/deb/filters &&
	mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/filters/filter-bundle-${version}.ear ${WORKSPACE_DIR}/repose/${version}/deb/filters/ &&
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/deb/etc ${WORKSPACE_DIR}/repose/${version}/deb/usr ${WORKSPACE_DIR}/repose/${version}/deb/var &&
	extract_archive ${WORKSPACE_DIR}/repose/${version}/deb/filters/filter-bundle-${version}.ear &&
	find . -name "*-${version}.jar" -prune -o -name "checker-core-*.jar" -prune -o -name "*.jar" -exec rm {} \;
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/deb/
echo -en "Retrieving DEB Extensions v${version}... "
wget -q ${DEB_REPO}/repose-extensions-filter-bundle/repose-extensions-filter-bundle_${version}_all.deb
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	ar p repose-extensions-filter-bundle_${version}_all.deb data.tar.gz | tar zx &&
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/deb/extensions &&
	mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/filters/extensions-filter-bundle-${version}.ear ${WORKSPACE_DIR}/repose/${version}/deb/extensions/ &&
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/deb/etc ${WORKSPACE_DIR}/repose/${version}/deb/usr ${WORKSPACE_DIR}/repose/${version}/deb/var &&
	extract_archive ${WORKSPACE_DIR}/repose/${version}/deb/extensions/extensions-filter-bundle-${version}.ear &&
	find . -name "*-${version}.jar" -prune -o -name "checker-core-*.jar" -prune -o -name "*.jar" -exec rm {} \;
else
	echo -en "FAILED\n"
fi
cd ${WORKSPACE_DIR}/repose/${version}/deb/
echo -en "Retrieving DEB CLI Utils v${version}... "
wget -q ${DEB_REPO}/repose-cli-utils/repose-cli-utils_${version}_all.deb
if (( "$?" == "0" )); then
	echo -en "SUCCESS\n"
	ar p repose-cli-utils_${version}_all.deb data.tar.gz | tar zx &&
	mkdir -p ${WORKSPACE_DIR}/repose/${version}/deb/cli-utils &&
	[ -f ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/repose-cli.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/repose/repose-cli.jar ${WORKSPACE_DIR}/repose/${version}/deb/cli-utils/cli-utils-${version}.jar
	[ -f ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/lib/repose/repose-cli.jar ] && mv ${WORKSPACE_DIR}/repose/${version}/deb/usr/share/lib/repose/repose-cli.jar ${WORKSPACE_DIR}/repose/${version}/deb/cli-utils/cli-utils-${version}.jar
	rm -Rf ${WORKSPACE_DIR}/repose/${version}/deb/etc ${WORKSPACE_DIR}/repose/${version}/deb/usr ${WORKSPACE_DIR}/repose/${version}/deb/var
else
	echo -en "FAILED\n"
fi
########################################################################################################################
echo -en "\n\nRetrieving MVN v${version}...\n"
mkdir -p ${WORKSPACE_DIR}/repose/${version}/mvn/
TYPES=( "jar" "ear" "war" )
for type in ${TYPES[*]} ; do
	for package in ${PACKAGES[*]} ; do
		ARTIFACTS=`curl -s "${MVN_SEARCH}?r=releases&g=${package}&v=${version}&p=${type}"`
		if (( 0 < `echo ${ARTIFACTS} | xmllint --xpath '//totalCount' - | sed 's/<totalCount>//g' | sed $'s/<\/totalCount>/\\\n/g' | grep -v '^$'` )); then
			ARTIFACTS=`echo ${ARTIFACTS} |
			xmllint --xpath '//artifactId' - |
			sed 's/<artifactId>//g' |
			sed $'s/<\/artifactId>/\\\n/g' |
			grep -v '^$' |
			sort`
			echo "Type:      ${type}"
			echo "Package:   ${package}"
			echo "Artifacts:"
			for artifact in ${ARTIFACTS[*]} ; do
				echo -en "   ${artifact} ... "
				mkdir -p ${WORKSPACE_DIR}/repose/${version}/mvn/${artifact}
				cd ${WORKSPACE_DIR}/repose/${version}/mvn/${artifact}/
				wget -q "${MVN_CONTENT}?r=releases&g=${package}&v=${version}&p=${type}&a=${artifact}" --content-disposition
				if (( "$?" == "0" )); then
					echo -en "SUCCESS\n"
				elif [[ "${type}" == "jar" && ("${artifact}" == "web-application" || "${artifact}" == "mocks-servlet" || "${artifact}" == "test-service-mock") ]]; then
					echo -en "SKIPPED\n"
				else
					echo -en "FAILED\n"
				fi
			done # artifact in ${ARTIFACTS[*]}
		fi
	done # package in ${PACKAGES[*]}
done # type in ${TYPES[*]}
########################################################################################################################
FILES=($(find ${WORKSPACE_DIR}/repose/${version}/mvn -maxdepth 2 -type f -name "*.[jew]ar"))
for file in ${FILES[*]} ; do
	echo -en "\n\nTesting ${file##*/}" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
	KNOWNS=($(find ${WORKSPACE_DIR}/repose/${version}/{rpm,deb} -name ${file##*/} -print))
	if (( 0 < ${#KNOWNS[@]} )); then
		echo -en "\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
		for known in ${KNOWNS[*]} ; do
			echo -en "   ${known} ... " | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
			echo -en "\n   " >> ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
			diff ${file} ${known} >> ${WORKSPACE_DIR}/repose/${version}/verify.txt 2>&1
			if (( "$?" == "0" )); then
				echo -en "SUCCESS\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
			else
				echo -en "\n      Re-Checking with archives extracted ... " | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
				echo -en "\n" >> ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
				if [ ! -d ${file}_extract ]; then
					extract_archive ${file}
				fi
				if [ ! -d ${known}_extract ]; then
					extract_archive ${known}
				fi
				diff -rq -x '\pom.properties' -x '*_extract' -x '*_decompile' ${file}_extract ${known}_extract >> ${WORKSPACE_DIR}/repose/${version}/verify.txt 2>&1
				if (( "$?" == "0" )); then
					echo -en "      SUCCESS - with Known differences\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
				elif [[ "${file##*.}" == "ear" || "${file##*.}" == "war" ]]; then
					echo -en "\n      Re-Checking with internal archives extracted ... " | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
					echo -en "\n" >> ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
					extract_archives ${file}_extract &&
					extract_archives ${known}_extract &&
					diff -rq -x '\pom.properties' -x '*.jar' -x '*_decompile' ${file}_extract ${known}_extract >> ${WORKSPACE_DIR}/repose/${version}/verify.txt 2>&1
					if (( "$?" == "0" )); then
						echo -en "      SUCCESS - No differences after extraction\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
					else
						echo -en "      Re-Checking with internal archives decompiled ... \n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
						decompile_jars ${file} &&
						decompile_jars ${known} &&
						diff -rq -x '\pom.properties' -x '*.jar' -x '*_extract' ${file}_extract ${known}_extract >> ${WORKSPACE_DIR}/repose/${version}/verify.txt 2>&1
						if (( "$?" == "0" )); then
							echo -en "      SUCCESS - No differences after decompilation\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
						else
							echo -en "      FAILURE!!!\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
						fi
					fi
				elif [ "${file##*.}" == "jar" ]; then
					echo -en "      Re-Checking with archives decompiled ... \n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt &&
					decompile_jar ${file} &&
					decompile_jar ${known} &&
					diff -r -x '\pom.properties' -x '*.jar' -x '*_extract' ${file}_decompile ${known}_decompile >> ${WORKSPACE_DIR}/repose/${version}/verify.txt 2>&1
					if (( "$?" == "0" )); then
						echo -en "      SUCCESS - No differences after decompilation\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
					else
						echo -en "      FAILURE!!!\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
					fi
				else
					echo -en "      FAILURE!!!\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
				fi
			fi
		done
	else
		echo -en "   NO KNOWN GOOD TO COMPARE\n" | tee -a  ${WORKSPACE_DIR}/repose/${version}/verify.txt
	fi
done
echo -en "Cleaning up v${version}...\n"
rm -Rf ${WORKSPACE_DIR}/repose/${version}/deb ${WORKSPACE_DIR}/repose/${version}/mvn ${WORKSPACE_DIR}/repose/${version}/rpm
STOP=$(date +"%s")
DIFF=$(($STOP-$START))
echo -en "\nTotal time: $(($DIFF / 60)) minutes and $(($DIFF % 60)) seconds\n"
echo -en "Finished at: $(date)\n"
#End of script
