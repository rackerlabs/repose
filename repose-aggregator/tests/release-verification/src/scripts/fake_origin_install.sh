echo "-------------------------------------------------------------------------------------------------------------------"
echo "Installing Fake Origin"
echo "-------------------------------------------------------------------------------------------------------------------"

cd /opt/fake-origin
echo `pwd`

# download and install the Fake Origin app dependencies
#
# The --unsafe-perm flag is used to support building the libxmljs library.
# Since there is no guarantee that libxmljs has been pre-compiled for the Docker image OS,
# node-gyp falls back to building the library. Since the Docker user is root, gyp
# creates a temporary directory for the build which breaks the Makefile. The flag
# makes things work again.
#
# TODO: If this fails, it does so silently. WRONG!
npm install --unsafe-perm

chmod 755 /opt/fake-origin
