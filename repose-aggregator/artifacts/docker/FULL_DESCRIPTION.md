## What is Repose?

Repose is an open-source, RESTful, middleware platform that transparently integrates with your existing infrastructure. Repose provides highly scalable and extensible solutions to API processing tasks including authentication, rate limiting, access control, logging, and more.

Please see our website for more information!
<http://www.OpenRepose.org/>

## Base Image

The primary images are built on top of an Ubuntu image. Ubuntu images are considered primary images because they are named by version number without qualification. Additionally, the latest image will always match the latest version of the Ubuntu-based image.

CentOS-based images are also provided for convenience. These images will be qualified with a suffix of `-centos`.

If for any reason a different base image would be useful to you, please let us know!

## How To Run This Image

This image will set up the suggested environment for running Repose. This includes installing Repose along with all of its dependencies, setting reasonable JVM options, exposing a service port, and declaring a volume for configuration.

Please see the Docker page in our documentation for more information, including specific commands to run!
<http://www.OpenRepose.org/versions/latest/recipes/docker.html>

## Tags

The tags which are currently maintained follow the format `x.x.x.x` or `x.x.x.x-centos`.

Tags that have already been published (e.g., older OS-specific tags) will continue to work, but there is no plan to maintain them going forward.

## Contact Us

Please contact us at <ReposeCore@Rackspace.com> with any questions or concerns!

## Other Information

Please see our documentation for more information on configuring and customizing your Repose instance!
<http://www.OpenRepose.org/versions/latest/>
