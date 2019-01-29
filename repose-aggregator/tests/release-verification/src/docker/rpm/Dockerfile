# To build this image, use a command like the following:
# docker build --build-arg REPOSE_VERSION=latest -t repose-container .
# To run the built image, use a command like the following:
# docker run --rm repose-container
FROM centos:7

MAINTAINER The Repose Team <reposecore@rackspace.com>

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# The service ports -- one for accepting HTTP traffic to Repose, and the other for connecting a JDWP debugger
# Note that these are not exposed to the host by default, but can be using the Docker CLI -p or -P flag.
EXPOSE 8080 10037

RUN yum update -y --quiet && yum install -y --quiet \
    git \
    ruby-devel \
    gcc \
    gcc-c++ \
    make \
    rpm-build \
    wget \
    curl \
    python-setuptools \
    java-1.8.0-openjdk-headless
RUN easy_install pip
RUN pip install --upgrade pip setuptools
RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo https://nexus.openrepose.org/repository/el/openrepose.repo

# Note: The COPY instruction was used rather than VOLUME since the behave of VOLUME varies based on the host OS
COPY etc_repose /release-verification/etc_repose
COPY fake-services /release-verification/fake-services
COPY scripts /release-verification/scripts
COPY *.rpm /release-verification/

RUN rm /release-verification/DELETE-ME.rpm

RUN sh /release-verification/scripts/node_install.sh
RUN npm install --unsafe-perm libxmljs

RUN sh /release-verification/scripts/fake_keystone_prepare.sh
RUN cp /release-verification/fake-services/fake-keystone2/package.json /opt/fake-keystone/package.json
RUN cp /release-verification/fake-services/fake-keystone2/app.js /opt/fake-keystone/app.js
RUN sh /release-verification/scripts/fake_keystone_install.sh

RUN sh /release-verification/scripts/fake_origin_prepare.sh
RUN cp /release-verification/fake-services/fake-origin/package.json /opt/fake-origin/package.json
RUN cp /release-verification/fake-services/fake-origin/app.js /opt/fake-origin/app.js
RUN sh /release-verification/scripts/fake_origin_install.sh

RUN yum update -y --quiet && sh /release-verification/scripts/repose_install_rpm.sh $REPOSE_VERSION
RUN cp --force /release-verification/etc_repose/* /etc/repose/

CMD ["/bin/bash", "/release-verification/scripts/verify.sh"]
