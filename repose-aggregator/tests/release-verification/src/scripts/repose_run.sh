rm -Rf /var/log/repose/* \
       /var/repose/* &&
systemctl daemon-reload
systemctl start repose-valve.service
systemctl enable repose-valve.service
