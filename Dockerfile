# Multi-stage Dockerfile for Repose with Java 11
# This addresses CVE-2023-41993 and other Java 8 vulnerabilities

# Stage 1: Build Repose with Java 11
FROM eclipse-temurin:11-jdk-jammy AS builder

WORKDIR /build

# Copy project files
COPY . .

# Ensure gradlew is executable
RUN chmod +x ./gradlew

# Build Repose with Gradle (skip tests and integration tests for faster build)
RUN ./gradlew clean buildAll -x test -x integrationTest --no-daemon

# Stage 2: Runtime image with Eclipse Temurin 11 JRE
FROM eclipse-temurin:11-jre-jammy

# Maintainer and labels
LABEL maintainer="The RBA Team <rba@rackspace.com>"
LABEL description="Repose 9.1.0.5 with Java 11 - Security patched version"
LABEL version="9.1.0.5-java11"
LABEL security.cve-fixed="CVE-2023-41993"

# Environment variables
ENV APP_ROOT=/etc/repose \
    APP_VARS=/var/repose \
    APP_LOGS=/var/log/repose \
    JAVA_OPTS=

# System prep: policy-rc.d, initctl diversion, apt config
# These optimizations reduce image size and improve build performance
RUN set -xe && \
    echo '#!/bin/sh' > /usr/sbin/policy-rc.d && \
    echo 'exit 101' >> /usr/sbin/policy-rc.d && \
    chmod +x /usr/sbin/policy-rc.d && \
    dpkg-divert --local --rename --add /sbin/initctl && \
    cp -a /usr/sbin/policy-rc.d /sbin/initctl && \
    sed -i 's/^exit.*/exit 0/' /sbin/initctl && \
    echo 'force-unsafe-io' > /etc/dpkg/dpkg.cfg.d/docker-apt-speedup && \
    echo 'DPkg::Post-Invoke { "rm -f /var/cache/apt/archives/*.deb /var/cache/apt/archives/partial/*.deb /var/cache/apt/*.bin || true"; };' > /etc/apt/apt.conf.d/docker-clean && \
    echo 'APT::Update::Post-Invoke { "rm -f /var/cache/apt/archives/*.deb /var/cache/apt/archives/partial/*.deb /var/cache/apt/*.bin || true"; };' >> /etc/apt/apt.conf.d/docker-clean && \
    echo 'Dir::Cache::pkgcache ""; Dir::Cache::srcpkgcache "";' >> /etc/apt/apt.conf.d/docker-clean && \
    echo 'Acquire::Languages "none";' > /etc/apt/apt.conf.d/docker-no-languages && \
    echo 'Acquire::GzipIndexes "true"; Acquire::CompressionTypes::Order:: "gz";' > /etc/apt/apt.conf.d/docker-gzip-indexes && \
    echo 'Apt::AutoRemove::SuggestsImportant "false";' > /etc/apt/apt.conf.d/docker-autoremove-suggests && \
    mkdir -p /run/systemd && echo 'docker' > /run/systemd/container

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

# Turn off local file logging (logs will go to stdout/stderr for Docker)
RUN if [ -f ${APP_ROOT}/log4j2.xml ]; then \
        sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml && \
        sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml; \
    fi

# Arbitrary User ID support for OpenShift and other container platforms
RUN chown -R repose:repose ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chgrp -R 0 ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS} ${APP_LOGS} /usr/share/repose

# Expose APP_ROOT as a volume for mounting
WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

# Switch to non-root user for security
USER repose

# Expose the default Repose service port
# Map to different host ports using: docker run -p <host-port>:8080
EXPOSE 8080

# Start Repose
CMD java $JAVA_OPTS -jar /usr/share/repose/repose.jar -c /etc/repose
