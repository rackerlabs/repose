# Use CentOS as a base, to enable security patching for
# CentOS users.
# The version should be whatever version conforms with
# the managed CentOS image.
FROM centos:7

MAINTAINER The Repose Team <reposecore@rackspace.com>

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Repose (and Java) from the Debian package repository.
RUN yum update -q -y && yum install -q -y wget java-1.8.0-openjdk-headless
RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo
RUN yum update -q -y && yum install -q -y repose-valve-$REPOSE_VERSION repose-filter-bundle-$REPOSE_VERSION repose-extensions-filter-bundle-$REPOSE_VERSION repose-experimental-filter-bundle-$REPOSE_VERSION

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
