mkdir -p /etc/systemd/system/repose-valve.service.d/
cd /etc/systemd/system/repose-valve.service.d/
cat > local.conf << EOF
[Service]
Environment="SAXON_HOME=/etc/saxon"
EOF

mkdir -p /etc/saxon
chmod 755 /etc/saxon
cd /etc/saxon
cat > saxon-license.lic << EOF
Feed me a license. NOM NOM NOM
EOF
chmod 644 /etc/saxon/saxon-license.lic
