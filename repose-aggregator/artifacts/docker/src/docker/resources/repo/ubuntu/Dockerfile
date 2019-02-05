# Use Ubuntu as a base, since it works and is familiar.
# The version should be the latest tested, LTS version.
FROM ubuntu:18.04

MAINTAINER The Repose Team <reposecore@rackspace.com>

# Install Java from an APT package repository.
RUN apt-get update -qq && apt-get install -qq -y apt-transport-https ca-certificates wget openjdk-8-jre-headless

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Repose from an APT package repository.
RUN wget --quiet -O - https://nexus.openrepose.org/repository/el/RPM_GPG_KEY-openrepose | apt-key add - && echo 'deb https://nexus.openrepose.org/repository/debian stable main' > /etc/apt/sources.list.d/openrepose.list
RUN apt-get update -qq && apt-get install -y repose=$REPOSE_VERSION repose-filter-bundle=$REPOSE_VERSION repose-extensions-filter-bundle=$REPOSE_VERSION repose-experimental-filter-bundle=$REPOSE_VERSION

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
