#!/bin/bash
#REPOSE_DIR=<REPOSE_DIR>/scripts
SCRIPT_DIR=$( cd "$( dirname "$0" )" && pwd )
REPOSE_DIR=$SCRIPT_DIR/..
TARGET_DIR=$REPOSE_DIR/target

cd $REPOSE_DIR
mkdir -p $TARGET_DIR

echo "Building the Maven Dependency Tree..."
mvn dependency:tree > $TARGET_DIR/mvn_dependency_tree.out

# All of the known and documented direct dependencies.
GROUP_IDS=(
    com.typesafe.akka
    commons-cli
    commons-codec
    commons-io
    commons-lang3
    org.apache.tomcat.embed
    xerces
    com.rackspace.papi.components.api-checker
    cglib
    org.rackspace
    net.sf.ehcache
    org.glassfish
    org.codehaus.groovy
    com.google.guava
    org.hamcrest
    org.apache.httpcomponents
    com.fasterxml.jackson.core
    javax
    javax.transaction
    javax.mail
    org.jvnet.jaxb2_commons
    org.jboss.spec
    com.sun.jersey
    org.eclipse.jetty
    joda-time
    com.github.fge
    junit
    org.linkedin
    log4j
    org.mockito
    com.mockrunner
    net.sourceforge.pjl-comp-filter
    org.powermock
    org.slf4j
    org.spockframework
    org.springframework
    xalan
    xmlunit
    com.yammer.metrics
)

echo ""
echo "================================================================================"
echo "All of the known direct dependencies that need the"
echo "version numbers confirmed against what is documented:"
echo "--------------------------------------------------------------------------------"
for groupId in ${GROUP_IDS[*]} ; do
    egrep "\[INFO\] \\+\\- " $TARGET_DIR/mvn_dependency_tree.out | cut -d' ' -f3 | cut -d':' -f1-4 | sort -u | grep "${groupId}:"
done

# Build the exclusion set based on all known direct dependencies.
EXCLUDE=
for groupId in ${GROUP_IDS[*]} ; do
    EXCLUDE="$EXCLUDE -e ${groupId}:"
done

echo ""
echo "================================================================================"
echo "All direct dependencies that are not currently documented:"
echo "--------------------------------------------------------------------------------"
egrep "\[INFO\] \\+\\- " $TARGET_DIR/mvn_dependency_tree.out | cut -d' ' -f3 | cut -d':' -f1-4 | sort -u | grep -v -e org.openrepose $EXCLUDE

# All of the UN-known and UN-documented direct dependencies.
GROUP_IDS_TOO=`egrep "\[INFO\] \\+\\- " $TARGET_DIR/mvn_dependency_tree.out | cut -d' ' -f3 | sort -u | grep -v -e org.openrepose $EXCLUDE | cut -d":" -f1 | sort -u`

EXCLUDE_TOO=
for groupId in ${GROUP_IDS_TOO[*]} ; do
    EXCLUDE_TOO="$EXCLUDE_TOO -e $groupId"
done

# These were determined to not be of importance at this time.
#echo ""
#echo "================================================================================"
#echo "All transient dependencies that are not already documented as direct dependencies:"
#echo "--------------------------------------------------------------------------------"
#egrep "\[INFO\] \|" $TARGET_DIR/mvn_dependency_tree.out | cut -d'-' -f2- | cut -d' ' -f2- | sort -u | grep -v -e org.openrepose $EXCLUDE $EXCLUDE_TOO
