# Use CentOS as a base, to enable security patching for CentOS users.
# The version should be whatever version conforms with the managed CentOS image.
FROM centos:7

MAINTAINER The Repose Team <reposecore@rackspace.com>

# Install Java from a Yum package repository.
RUN yum update -q -y && yum install -q -y wget java-1.8.0-openjdk-headless

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Repose from a Yum package repository.
RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo https://nexus.openrepose.org/repository/el/openrepose.repo
RUN yum update -q -y && yum install -q -y repose-$REPOSE_VERSION repose-filter-bundle-$REPOSE_VERSION repose-extensions-filter-bundle-$REPOSE_VERSION repose-experimental-filter-bundle-$REPOSE_VERSION

ENV APP_ROOT=/etc/repose
ENV APP_VARS=/var/repose
ENV APP_LOGS=/var/log/repose

# Turn off local logging
RUN sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml
RUN sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml

# Arbitrary User ID support
RUN chgrp -R 0 ${APP_ROOT} ${APP_VARS} ${APP_LOGS} && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS} ${APP_LOGS}

# Expose APP_ROOT as a volume for mounting.
WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

# Switch user to repose
USER repose

# Expose the default Repose service port for host port forwarding.
# If the port in the user's system model differs from this port, the user will have to map it manually using the
# "-p" flag with the Docker run command.
EXPOSE 8080

# This environment variable is used to set command-line options.
# The user can manually set these options using the "-e" flag with the Docker run command.
ENV JAVA_OPTS=

# Start Repose.
CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
