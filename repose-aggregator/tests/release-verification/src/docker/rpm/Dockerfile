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
    make \
    rpm-build \
    wget \
    curl \
    python-setuptools
RUN easy_install pip
RUN pip install --upgrade pip setuptools
RUN git clone https://github.com/bmc/daemonize.git
RUN cd daemonize && \
    sh configure && \
    make && \
    make install
RUN ln -s /usr/local/sbin/daemonize /usr/sbin/daemonize
RUN gem install fpm
RUN /usr/local/bin/fpm -s empty -t rpm -n daemonize -v 0.0.0 --iteration 0 --architecture noarch
RUN rpm --install --ignorearch daemonize-0.0.0-0.noarch.rpm
RUN wget --quiet https://rpm.nodesource.com/pub_0.12/el/7/x86_64/nodejs-0.12.7-1nodesource.el7.centos.x86_64.rpm
RUN rpm -Uh --quiet nodejs-0.12.7-1nodesource.el7.centos.x86_64.rpm
RUN yum install -y --quiet gcc-c++ make
RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo

COPY etc_repose /release-verification/etc_repose
COPY fake-services /release-verification/fake-services
COPY scripts /release-verification/scripts
COPY pkgs /release-verification/pkgs

RUN sh /release-verification/scripts/fake_keystone_prepare.sh
RUN cp /release-verification/fake-services/fake-keystone2/package.json /opt/fake-keystone/package.json
RUN cp /release-verification/fake-services/fake-keystone2/app.js /opt/fake-keystone/app.js
RUN sh /release-verification/scripts/docker_fake_keystone_install.sh

RUN sh /release-verification/scripts/fake_origin_prepare.sh
RUN cp /release-verification/fake-services/fake-origin/package.json /opt/fake-origin/package.json
RUN cp /release-verification/fake-services/fake-origin/app.js /opt/fake-origin/app.js
RUN sh /release-verification/scripts/docker_fake_origin_install.sh

RUN yum update -y --quiet && sh /release-verification/scripts/repose_install_rpm.sh $REPOSE_VERSION
RUN cp --force /release-verification/etc_repose/* /etc/repose/

CMD ["/bin/bash", "/release-verification/scripts/docker_verify.sh"]
