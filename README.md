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

#REPOSE - The REstful PrOxy Service Engine#

Repose is an open-source platform that you can use to build stacks of reusable software
components. These components can be leveraged by service developers to perform
common API processing tasks. By using Repose's components rather than creating their
own, service developers can focus on the unique features of their services.  

Repose can be used inside a service to perform API operations. It can also run on one or
more separate servers as a proxy to one or more services. At its core, Repose is a proxy
that allows services to use Enterprise Integration Patterns (EIP).

For more information, see our [website](http://openrepose.org/) and [wiki](http://wiki.openrepose.org).


##Repose Components##

Completed Repose components include:

 * Client Authentication
 * Rate Limiting
 * Versioning
 * HTTP Logging

Repose components that are currently being developed include:

 * Service Authentication
 * Content Normalization
 * Translation

Repose components that are planned for future development include:

 * Content Negotiation
 * Contract Scope Filter
 * Client Authorization


##Benefits##

 * **Scalable**. Repose is incredibly scalable because it is designed to be stateless, allowing state to be 
  distributed across the web.
 * **Flexible**. Repose can be run as an executable JAR, deployed as a WAR file in any Servlet container, or 
  deployed on a separate server. Repose's configuration allows a user to define which components to use 
  and details for each component.
 * **Extensible**. New components can easily be added to Repose.
 * **High performance**. Repose can handle high loads with high accuracy.
 * **Improving**. Repose is under development and actively being worked on.

 
##Installation##
You can install/run Repose by several methods:

- Embedded via the source code (JAR)
- ROOT WAR
- Proxy Server (via Resource Package Manager (RPM) or via Debian Package (DEB))
- Proxy Server Cluster


###Embedded Deployment Method###

In an Embedded Repose deployment, Repose is embedded in the other service’s WAR using 
JEE Specification.  The service host also hosts all of Repose's components in the same 
app container. The servlet container may be Tomcat, Jetty, Glassfish, etc.

This deployment option requires integration with the application code and is not as 
flexible as the other deployment methods.  For this reason, this is not the recommended
deployment option.


###ROOT WAR Deployment Method###

With the Root WAR Repose deployment, the Repose Root WAR replaces the root component of the 
servlet container. The servlet container may be Tomcat, Jetty, Glassfish, etc.


###Proxy Server Deployment Method###

In the Proxy Server deployment, Repose is in an external servlet container. This allows 
host level routing over the network, so a non-Java service can take advantage of the 
Repose features.


###Proxy Server Cluster Deployment Method###

Using the Power Proxy Cluster deployment, Repose may be scaled across multiple hosts. This 
allows faster processing. Auto-balance caching between the nodes will occur on the basis of 
resources and requests. (Rate Limiting is currently the only component that is able to take 
advantage of this. For all other components auto-balance caching does not matter.)


##Configuration##

###Configuration Features###

Repose supports the following features for configuration management:
    Runtime updates
    Fine grained resource locking


###Configuration Expectations###

Repose will search for configurations in a user specified directory.
    The configuration root directory must be readable (chmod 755)
    The configuration files should be user readable and writable (chmod 600)

Setting the Configuration Root Directory
    Using web.xml, the web XML should contain:

    <context-param>
        <param-name>powerapi-config-directory</param-name>
        <param-value>/etc/repose/</param-value>
    </context-param>

###Configuration Mappings###

Each Repose component specifies a unique configuration name. The component to configuration 
name mappings are listed below.
    _Component_      _Configuration Name_
    System           system-model.cfg.xml
    Rate Limiting    rate-limiting.cfg.xml
    Versioning       versioning.cfg.xml    
    Translation      translation.cfg.xml
    Authentication   client-auth-n.cfg.xml


##Repose Documentation##
Documentation is included with the source files and may be built with the maven command:  
<pre>
    export MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=256m'
    mvn clean install -Pdocbook
</pre>
This will build the documentation pdfs in the generated "target/docbkx/" directory.



##Notes Regarding Licensing##


All files contained with this distribution of Repose are licenced either
under the Apache License v2.0 (http://www.apache.org/licenses/LICENSE-2.0) or
the GNU General Public License v2.0 (http://www.gnu.org/licenses/gpl-2.0.html).
You must agree to the terms of these licenses and abide by them before
viewing, utilizing, modifying, or distributing the source code contained
within this distribution.

