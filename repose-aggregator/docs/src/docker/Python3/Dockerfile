FROM python:3

USER root
RUN pip install git+https://github.com/rackerlabs/ascii_binder_search_plugin

LABEL url="https://github.com/gluster/ascii_binder_search_plugin" \
      summary="a documentation search plugin for AsciiBinder" \
      description="AsciiBinder Search Plugin is for generating versioned document search files. Run this container image from the documentation repository, which is mounted into the container. Note: Generated files will be owned by root if you don't use Docker's --user option." \
      RUN="docker run -it \
          -u $(stat -c '%u:%g' $(pwd)) \
          -v $(pwd):/docs:z \
          IMAGE"

WORKDIR /docs
CMD ascii_binder_search -i front_end_indexer -v
