# Use Ubuntu as a base, since it works and is familiar.
# The version should be the latest tested, LTS version.
FROM ubuntu:16.04

MAINTAINER The Repose Team <reposecore@rackspace.com>

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Repose (and Java) from the Debian package repository.
RUN apt-get update -qq && apt-get install -qq -y wget openjdk-8-jre-headless
RUN wget --quiet -O - http://repo.openrepose.org/debian/pubkey.gpg | apt-key add - && echo 'deb http://repo.openrepose.org/debian stable main' > /etc/apt/sources.list.d/openrepose.list
RUN apt-get update -qq && apt-get install -y repose-valve=$REPOSE_VERSION repose-filter-bundle=$REPOSE_VERSION repose-extensions-filter-bundle=$REPOSE_VERSION repose-experimental-filter-bundle=$REPOSE_VERSION

# Expose /etc/repose as a volume for mounting.
WORKDIR /etc/repose
VOLUME /etc/repose

# Switch user to repose
USER repose

# Create the log file so that we can safely tail it as part of the CMD instruction.
RUN touch /var/log/repose/current.log

# Expose the default Repose service port for host port forwarding.
# If the port in the user's system model differs from this port, the user will have to map it manually using the
# "-p" flag with the Docker run command.
EXPOSE 8080

# Start Repose.
CMD ["java", "-jar", "/usr/share/repose/repose-valve.jar", "-c", "/etc/repose"]
