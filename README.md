<pre>
 ______     ______     ______   ______     ______     ______    
/\  == \   /\  ___\   /\  == \ /\  __ \   /\  ___\   /\  ___\ 
\ \  __/   \ \  __\   \ \  _-/ \ \ \/\ \  \ \___  \  \ \  __\
 \ \_\ \_\  \ \_____\  \ \_\    \ \_____\  \/\_____\  \ \_____\ 
  \/_/ /_/   \/_____/   \/_/     \/_____/   \/_____/   \/_____/
  

                    .'.-:-.`.
                    .'  :  `.
                    '   :   '   /
                 .------:--.   /
               .'           `./
        ,.    /            0  \
        \ ' _/                 )
~~~~~~~~~\. __________________/~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
</pre>

# REPOSE - The REstful PrOxy Service Engine #

Repose is an open-source platform that you can use to build stacks of reusable software
components. These components can be leveraged by service developers to perform
common API processing tasks. By using Repose's components rather than creating their
own, service developers can focus on the unique features of their services.  

Repose is run as a standalone application (either as a Linux service or using the
JAR directly). Repose can be run on the same server or on a different server, and
it can be run across multiple servers for horizontal scaling. At its core, Repose is a
proxy that allows services to use Enterprise Integration Patterns (EIP).

For more information, check out our [Getting Started with Repose](https://repose.atlassian.net/wiki/display/REPOSE/Getting+Started+with+Repose) guide.


## Benefits ##

* **Scalable**. Repose is incredibly scalable because it is designed to be stateless.
* **Flexible**. Repose is run as a [standalone Linux service (Valve)](https://repose.atlassian.net/wiki/display/REPOSE/Valve+Installation).
* **Extensible**. New [components](https://repose.atlassian.net/wiki/display/REPOSE/Filters+and+services)
  are being added all of the time, and you can even build your own
  [custom component](https://github.com/rackerlabs/repose-hello-world).
* **High performance**. Repose can handle [high loads](https://repose.atlassian.net/wiki/display/REPOSE/Performance+best+practices) with high accuracy.
* **Improving**. Repose continues to be under [active development](https://github.com/rackerlabs/repose/releases).


## Repose Components ##

Repose includes several [filters and services](https://repose.atlassian.net/wiki/display/REPOSE/Filters+and+services)
out of the box.  These include:

* [Rate Limiting](https://repose.atlassian.net/wiki/display/REPOSE/Rate+Limiting+filter)
* [Keystone v2 Auth](https://repose.atlassian.net/wiki/display/REPOSE/Keystone+v2+Filter)
* [OpenStack v3 Auth](https://repose.atlassian.net/wiki/display/REPOSE/OpenStack+Identity+v3+filter)
* [API Validation](https://repose.atlassian.net/wiki/display/REPOSE/API+Validation+filter)
* [Translation](https://repose.atlassian.net/wiki/display/REPOSE/Translation+filter)
* [Compression](https://repose.atlassian.net/wiki/display/REPOSE/Compression+filter)
* [CORS](https://repose.atlassian.net/wiki/display/REPOSE/CORS+Filter)
* [Versioning](https://repose.atlassian.net/wiki/display/REPOSE/Versioning+filter)
* [HTTP Logging](https://repose.atlassian.net/wiki/display/REPOSE/SLF4J+HTTP+Logging+filter)

Repose also makes it easy to create your own custom components.  Check out
our [example custom filter](https://github.com/rackerlabs/repose-hello-world) for more details.

 
## Installation ##
You can install Repose using the following methods:

* RPM using yum ([Valve](https://repose.atlassian.net/wiki/display/REPOSE/Valve+Installation#ValveInstallation-RHEL%28yum%29baseddistributions) or [WAR](https://repose.atlassian.net/wiki/display/REPOSE/WAR+Installation#WARInstallation-RHEL%28yum%29baseddistributions))
* DEB using apt-get ([Valve](https://repose.atlassian.net/wiki/display/REPOSE/Valve+Installation#ValveInstallation-Debian%28apt%29baseddistributions) or [WAR](https://repose.atlassian.net/wiki/display/REPOSE/WAR+Installation#WARInstallation-Debian%28apt%29baseddistributions))
* [Puppet](https://github.com/rackerlabs/puppet-repose)
* [Chef](https://github.com/rackerlabs/cookbook-repose)


## Configuration ##

Repose will search for configuration files in the user specified directory.

* The configuration root directory must be user readable.
* The configuration files should be user readable and writable.

Setting the Configuration Root Directory.

* Simply pass the configuration directory to the Java process using the "-c" option.


## Licensing ##

Original files contained with this distribution of Repose are licensed under
the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0).

You must agree to the terms of this license and abide by them before using,
modifying, or distributing Repose or the Repose source code contained within
this distribution.

Some dependencies are under other licenses.

By using, modifying, or distributing Repose you may also be subject to the
terms of those licenses.

See the full list of dependencies in DEPENDENCIES.txt.

By contributing to this project, you agree to abide to the terms and conditions
outlined in CONTRIBUTORS.txt.
