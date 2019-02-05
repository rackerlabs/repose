# Use CentOS as a base, to enable security patching for CentOS users.
# The version should be whatever version conforms with the managed CentOS image.
FROM centos:7

MAINTAINER The Repose Team <reposecore@rackspace.com>

# Setting constants
ENV APP_ROOT=/etc/repose
ENV APP_VARS=/var/repose
ENV APP_LOGS=/var/log/repose
ENV PKG_DIR=/tmp/packages/repose/

# This environment variable is used to set command-line options.
# The user can manually set these options using the "-e" flag with the Docker run command.
ENV JAVA_OPTS=

# Install Java from a package repository.
RUN yum update -q -y && yum install -q -y java-1.8.0-openjdk-headless

# Copy local Repose packages
COPY repose-*.rpm ${PKG_DIR}
COPY repose-filter-bundle-*.rpm ${PKG_DIR}
COPY repose-extensions-filter-bundle-*.rpm ${PKG_DIR}
COPY repose-experimental-filter-bundle-*.rpm ${PKG_DIR}

# Install Repose directly from packages
RUN yum install -y ${PKG_DIR}/*.rpm

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

# Start Repose.
CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
