FROM centos/ruby-27-centos7

RUN scl enable rh-ruby27 -- gem install listen -v 3.0.8
RUN scl enable rh-ruby27 -- gem install ascii_binder -v 0.1.15.1

USER root
RUN yum install -y java-1.8.0-openjdk && \
    yum clean all

LABEL url="http://www.asciibinder.org" \
      summary="a documentation system built on Asciidoctor" \
      description="AsciiBinder is for documenting versioned, interrelated projects. Run this container image from the documentation repository, which is mounted into the container. Note: Generated files will be owned by root if you don't use Docker's --user option." \
      RUN="docker run -it \
          -u $(stat -c '%u:%g' $(pwd)) \
          -v $(pwd):/docs:z \
          IMAGE"

ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.382.b05-1.el7_9.x86_64/jre/
ENV LANG=en_US.UTF-8
WORKDIR /docs
CMD asciibinder package --log-level=debug
