# Repose - Ubuntu 22.04 from package repository
# This Dockerfile installs Repose from a pre-built package repository.
# Requires REPOSE_VERSION build arg and a working APT repository.
#
# NOTE: The original nexus.openrepose.org repository is defunct.
# Update the repository URL below to point to your package host before use.
FROM eclipse-temurin:11-jre-jammy

LABEL maintainer="The Repose Team <reposecore@rackspace.com>"
LABEL description="Repose - The REstful PrOxy Service Engine (repo install)"
LABEL base.os="Ubuntu 22.04 (Jammy)"

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

RUN apt-get update -qq && apt-get install -qq -y \
        apt-transport-https \
        ca-certificates \
        wget \
        gnupg \
    && rm -rf /var/lib/apt/lists/*

# TODO: Replace this URL with your actual package repository
# The original nexus.openrepose.org is no longer available.
# RUN wget --quiet -O - https://your-repo.example.com/RPM_GPG_KEY-openrepose | apt-key add - && \
#     echo 'deb https://your-repo.example.com/repository/debian stable main' > /etc/apt/sources.list.d/openrepose.list
# RUN apt-get update -qq && apt-get install -y \
#     repose=$REPOSE_VERSION \
#     repose-filter-bundle=$REPOSE_VERSION \
#     repose-extensions-filter-bundle=$REPOSE_VERSION \
#     repose-experimental-filter-bundle=$REPOSE_VERSION

ENV APP_ROOT=/etc/repose \
    APP_VARS=/var/repose \
    APP_LOGS=/var/log/repose \
    JAVA_OPTS=

# Turn off local logging
RUN if [ -f ${APP_ROOT}/log4j2.xml ]; then \
        sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml && \
        sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml; \
    fi

# Arbitrary User ID support
RUN chgrp -R 0 ${APP_ROOT} ${APP_VARS} ${APP_LOGS} && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS} ${APP_LOGS}

WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

USER repose

EXPOSE 8080

CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
