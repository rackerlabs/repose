# Repose - Rocky Linux 9 with Eclipse Temurin 11
# Replaces the legacy CentOS 7 Dockerfile (CentOS 7 EOL: June 2024)
# Multi-stage build: compile from source, run on minimal JRE
FROM eclipse-temurin:11-jdk-jammy AS builder

WORKDIR /build

COPY . .

RUN chmod +x ./gradlew

# Build Repose (skip tests for image builds)
RUN ./gradlew clean buildAll -x test -x integrationTest --no-daemon

# Runtime stage - Rocky Linux 9 (RHEL-compatible CentOS successor)
FROM rockylinux:9-minimal

LABEL maintainer="The Repose Team <reposecore@rackspace.com>"
LABEL description="Repose - The REstful PrOxy Service Engine"
LABEL base.os="Rocky Linux 9"

ENV APP_ROOT=/etc/repose \
    APP_VARS=/var/repose \
    APP_LOGS=/var/log/repose \
    JAVA_OPTS=

# Install OpenJDK 11 JRE from Rocky's AppStream
RUN microdnf install -y --nodocs \
        java-11-openjdk-headless \
        shadow-utils \
    && microdnf clean all

# Create repose user and group
RUN groupadd -r repose && \
    useradd -r -g repose -d /home/repose -s /bin/bash repose && \
    mkdir -p /home/repose && \
    chown -R repose:repose /home/repose

# Create necessary directories
RUN mkdir -p ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose/filters

# Copy built artifacts from builder stage
COPY --from=builder /build/repose-aggregator/artifacts/valve/build/libs/repose.jar /usr/share/repose/
COPY --from=builder /build/repose-aggregator/artifacts/filter-bundle/build/libs/*.ear /usr/share/repose/filters/
COPY --from=builder /build/repose-aggregator/artifacts/extensions-filter-bundle/build/libs/*.ear /usr/share/repose/filters/
COPY --from=builder /build/repose-aggregator/artifacts/experimental-filter-bundle/build/libs/*.ear /usr/share/repose/filters/
COPY --from=builder /build/repose-aggregator/artifacts/valve/src/config/filters/*.xml ${APP_ROOT}/
COPY --from=builder /build/repose-aggregator/artifacts/valve/src/config/filters/*.cfg.xml ${APP_ROOT}/

# Turn off local file logging (logs go to stdout/stderr for Docker)
RUN if [ -f ${APP_ROOT}/log4j2.xml ]; then \
        sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml && \
        sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml; \
    fi

# Arbitrary User ID support (OpenShift compatibility)
RUN chown -R repose:repose ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chgrp -R 0 ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose

WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

USER repose

EXPOSE 8080

CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
