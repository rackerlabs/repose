# Repose - Rocky Linux 9 from package repository
# Replaces the legacy CentOS 7 Dockerfile (CentOS 7 EOL: June 2024)
# This Dockerfile installs Repose from a pre-built package repository.
# Requires REPOSE_VERSION build arg and a working Yum/DNF repository.
#
# NOTE: The original nexus.openrepose.org repository is defunct.
# Update the repository URL below to point to your package host before use.
FROM rockylinux:9-minimal

LABEL maintainer="The Repose Team <reposecore@rackspace.com>"
LABEL description="Repose - The REstful PrOxy Service Engine (repo install)"
LABEL base.os="Rocky Linux 9"

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Java runtime
RUN microdnf install -y --nodocs \
        java-11-openjdk-headless \
        shadow-utils \
        wget \
    && microdnf clean all

# TODO: Replace this URL with your actual package repository
# The original nexus.openrepose.org is no longer available.
# RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo https://your-repo.example.com/el/openrepose.repo
# RUN microdnf install -y \
#     repose-$REPOSE_VERSION \
#     repose-filter-bundle-$REPOSE_VERSION \
#     repose-extensions-filter-bundle-$REPOSE_VERSION \
#     repose-experimental-filter-bundle-$REPOSE_VERSION

ENV APP_ROOT=/etc/repose \
    APP_VARS=/var/repose \
    APP_LOGS=/var/log/repose \
    JAVA_OPTS=

# Create repose user and group
RUN groupadd -r repose && \
    useradd -r -g repose -d /home/repose -s /bin/bash repose && \
    mkdir -p /home/repose && \
    chown -R repose:repose /home/repose

# Create necessary directories
RUN mkdir -p ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose/filters

# Turn off local logging
RUN if [ -f ${APP_ROOT}/log4j2.xml ]; then \
        sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml && \
        sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml; \
    fi

# Arbitrary User ID support
RUN chown -R repose:repose ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chgrp -R 0 ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose

WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

USER repose

EXPOSE 8080

CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
