FROM python:3

USER root

LABEL url="https://docs.python.org/3/library/http.server.html" \
      summary="a basic search plugin for AsciiBinder" \
      description="Serves files from the volume mounted as /public_html and below, directly mapping the directory structure to HTTP requests." \
      RUN="docker run \
          --interactive --tty \
          --publish 8000:8000 \
          --volume $(pwd):/public_html:z \
          IMAGE"

EXPOSE 8000
CMD python3 -m http.server --directory /public_html
