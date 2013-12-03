#Functional Tests


Modules to help out with creating functional tests for Repose that take advantage of Repose's embedded functionality.

##Glassfish-Support & Tomcat-Support

These two modules generate a executable jar which can be used to deploy Repose under a Tomcat or Glassfish Environment.

Invocation: java -jar test-**Tomcat | Glassfish**-jar-with-dependencies.jar -p 10001 -w **ReposeWar** -s 10002 -war **OtherWar**
-p Listening port for container
-s Stop port for container
-war Other war files to be deployed. Currently, only the Tomcat-Support module supports this feature.

##Mocks-Servlet

The Mocks servlet can act as a test Origin Service for Repose. When hit with a request the servlet will respond
with a response body that contains an xml payload that is a representation of the request it received.

Example:
request: curl 'localhost:8080/path/to/resource?query=param' -d 'Request Body' -H "header1:value1" -H "header2:value2"

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;request-info xmlns="http://openrepose.org/repose/httpx/v1.0"&gt;
  &lt;method&gt;POST&lt;/method&gt;
  &lt;path&gt;/path/to/resource&lt;/path&gt;
  &lt;uri&gt;http://localhost:8080/path/to/resource&lt;/uri&gt;
  &lt;queryString&gt;query=param&lt;/queryString&gt;
  &lt;query-params&gt;
    &lt;parameter value="[param]" name="query"/&gt;
  &lt;/query-params&gt;
  &lt;headers&gt;
    &lt;header value="curl/7.24.0 (x86_64-apple-darwin12.0) libcurl/7.24.0 OpenSSL/0.9.8y zlib/1.2.5" name="user-agent"/&gt;
    &lt;header value="localhost:8080" name="host"/&gt;
    &lt;header value="*/*" name="accept"/&gt;
    &lt;header value="value1" name="header1"/&gt;
    &lt;header value="value2" name="header2"/&gt;
    &lt;header value="12" name="content-length"/&gt;
    &lt;header value="application/x-www-form-urlencoded" name="content-type"/&gt;
  &lt;/headers&gt;
  &lt;body&gt;Request Body&lt;/body&gt;
&lt;/request-info&gt;

###Mocks-Support


The mocks-support module helps with comprehending the response from the mocks-servlet. Using the MocksUtil utility class the user can convert the sent xml payload into a usable Java Object, RequestInfo.
