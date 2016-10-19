# To build this image, use a command like the following:
# docker build --build-arg REPOSE_VERSION=latest -t repose-container .
# To run the built image, use a command like the following:
# docker run --rm repose-container
FROM ubuntu:xenial

MAINTAINER The Repose Team <reposecore@rackspace.com>

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# The service ports -- one for accepting HTTP traffic to Repose, and the other for connecting a JDWP debugger
# Note that these are not exposed to the host by default, but can be using the Docker CLI -p or -P flag.
EXPOSE 8080 10037

RUN apt-get update -qq && apt-get install -y -qq \
    wget \
    curl \
    python-pip
RUN wget --quiet http://mirrors.kernel.org/ubuntu/pool/universe/r/rlwrap/rlwrap_0.41-1build1_amd64.deb
RUN dpkg -i rlwrap_0.41-1build1_amd64.deb
RUN wget --quiet https://deb.nodesource.com/node_0.12/pool/main/n/nodejs/nodejs_0.12.7-1nodesource1~trusty1_amd64.deb
RUN dpkg -i nodejs_0.12.7-1nodesource1~trusty1_amd64.deb
RUN apt-mark hold nodejs
RUN apt-get install -y -f
RUN wget -O - http://repo.openrepose.org/debian/pubkey.gpg | apt-key add -
RUN echo "deb http://repo.openrepose.org/debian stable main" > /etc/apt/sources.list.d/openrepose.list

# TODO: Should these copy commands be replaced by volume mounting from the command line?
COPY etc_repose /etc_repose
COPY fake-services /fake-services
COPY scripts /scripts
COPY *.deb /

RUN sh /scripts/fake_keystone_prepare.sh
RUN cp fake-services/fake-keystone2/package.json /opt/fake-keystone/package.json
RUN cp fake-services/fake-keystone2/app.js /opt/fake-keystone/app.js
RUN sh /scripts/docker_fake_keystone_install.sh

RUN sh /scripts/fake_origin_prepare.sh
RUN cp fake-services/fake-origin/package.json /opt/fake-origin/package.json
RUN cp fake-services/fake-origin/app.js /opt/fake-origin/app.js
RUN sh /scripts/docker_fake_origin_install.sh

RUN apt-get update -qq && sh /scripts/repose_install_deb.sh $REPOSE_VERSION
RUN cp --force /etc_repose/* /etc/repose/
RUN sh /scripts/repose_run.sh

# TODO: Switch to an ENTRYPOINT to make the Dockerfile executable
CMD ["sh", "/scripts/docker_verify.sh"]
