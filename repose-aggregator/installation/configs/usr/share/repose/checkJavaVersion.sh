#!/bin/sh

# Validate that we have a java executable on the path
command -v $1 >/dev/null 2>&1
if [ $? -gt 0 ]; then
  echo "Repose requires the 'JAVA_REPOSE' property be set to the absolute path of a Java executable!"
  echo " - The current value ($1) is invalid."
  echo " - This can be updated in the configuration file: /lib/systemd/system/repose-valve.service.d/local.conf"
  echo "Exiting!"
  exit 2
fi

# We've found a java on the path, now validate that the minor version is adequate.
# We're not guaranteed to have bash on Debian flavors, so we have to use sh stuff.
VERSION=$($1 -version 2>&1 | grep ' version' | awk '{ print substr($3, 2, length($3)-2); }')
JAVA_MINOR=$(echo $VERSION | tr "." " " | cut -d " " -f2)

if [ "$JAVA_MINOR" -lt "8" ]; then
  echo "Repose requires a Java version of at least 8 to function."
  echo "Please install a JRE 1.8 or greater."
  exit 1
fi
echo "Java is at JRE 1.8 or greater."
