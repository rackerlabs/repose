The primary objective of this project is to provide an automated way of
verifying releases.

To accomplish that objective, a consistent, stable environment is provided
by Vagrant along with a supporting set of scripts. Gradle tasks provide
simple management of the Vagrant environment.

Tangentially, the scaffolding put in place by this project provides a
relatively simple way of standing up a Repose sandbox. A Repose sandbox
can be used for quickly troubleshooting issues, or experimenting with
different facets of Repose or its environment.

# Gradle Management
A number of Gradle tasks have been defined to either stand up a Repose
sandbox or run a smoke test against a sandboxed instance of Repose.
These tasks can be viewed using the `gradle tasks` command on this
project.

A number of Gradle properties are used as the parameters for the
Repose sandbox environment. These properties are described by the
table below. Remember that Gradle accepts properties by using the
following command form to invoke a Gradle task:
`gradle <task> -P <property-key>=<property-value>`

| Property Name   | Default Value          | Description |
| --------------- | ---------------------- | ----------- |
| config-dir      | $projectDir/src/config | The absolute path of the directory containing the configured files to be used by Repose. |
| release-version | local                  | The version of Repose to set up in the environment. |

The release-version property should either match a release tag in the
Repose repository, or be one of {local, latest}. A value of "local" will
build the project locally and set up the built artifacts. A value of
"current" will set up the latest published release.

# Verifying A Release
Gradle tasks have been set up to allow for verifying either the DEB
packages, the RPM packages, or both. To verify a release, simply
run `gradle smokeTest -Prelease-version=<version>` (e.g. 8.1.0.0) from
this project. If the build succeeds, then the release succeeded!

# Repose As A Sandbox
To start the sandbox, run the `vagrantUpDeb` or `vagrantUpRpm` task
with the desired properties. Once the sandbox is running, the environment
can be accessed directly by running the `vagrant ssh` command from the
directory containing the Vagrantfile (e.g., src/vagrant/deb relative to
this project directory if the `vagrantUpDeb` task was run). To debug the
instance of Repose running in the sandbox, connect a remote debugger to
port 18038 on the local host for Debian builds and 18039 for RPM builds.
This port is forwarded by Vagrant to connect to the JDWP port of Repose
in Vagrant's guest VM. The ports 18088 and 18089 for the Debian and RPM
builds are also exposed and are forwarded to the Repose port in Vagrant's
guest VM.
