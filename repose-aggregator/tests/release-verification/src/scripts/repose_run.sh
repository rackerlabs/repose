rm -Rf /var/log/repose/* \
       /var/repose/*
java -Xmx512M -Xms512M -jar /usr/share/repose/repose.jar -c /etc/repose > /release-verification/var-log-repose-current.log 2>&1 &
