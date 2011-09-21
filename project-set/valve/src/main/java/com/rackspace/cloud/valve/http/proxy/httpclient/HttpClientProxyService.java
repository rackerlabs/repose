/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.cloud.valve.http.proxy.httpclient;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.BufferedInputStream;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import com.rackspace.cloud.valve.http.proxy.ProxyService;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;

import static com.rackspace.cloud.valve.http.Headers.*;

/**
 *
 * @author John Hopper
 */
public class HttpClientProxyService implements ProxyService {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientProxyService.class);

    private final HttpConnectionManager manager;
    private final HttpClient client;
    private final HostConfiguration proxiedHost;

    public HttpClientProxyService(String targetHost) {
        proxiedHost = new HostConfiguration();
        try {
            proxiedHost.setHost(new URI(targetHost, false));
        } catch (URIException e) {
            LOG.error("Invalid target host url: " + targetHost, e);
        }

        manager = new MultiThreadedHttpConnectionManager();
        client = new HttpClient(manager);
    }

    @Override
    public int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String target = proxiedHost.getHostURL() + request.getRequestURI();
        final RequestProcessor processor = new RequestProcessor(request, proxiedHost);
        final ProcessableRequest method = newMethod(request.getMethod(), target);

        if (method != null) {
            HttpMethod processedMethod = method.process(processor);
            
            return executeProxyRequest(processedMethod, request, response);
        }

        //Something exploded; return a status code that doesn't exist
        return -1;
    }

    private ProcessableRequest newMethod(String method, String uri) {
        if (method.equalsIgnoreCase("GET")) {
            return new HttpMethodBaseWrapper(new GetMethod(uri));
        }

        if (method.equalsIgnoreCase("PUT")) {
            return new EntityEnclosingMethodWrapper(new PutMethod(uri));
        }

        if (method.equalsIgnoreCase("POST")) {
            return new EntityEnclosingMethodWrapper(new PostMethod(uri));
        }

        if (method.equalsIgnoreCase("DELETE")) {
            return new HttpMethodBaseWrapper(new DeleteMethod(uri));
        }

        if (method.equalsIgnoreCase("HEAD")) {
            return new HttpMethodBaseWrapper(new HeadMethod(uri));
        }

        if (method.equalsIgnoreCase("OPTIONS")) {
            return new HttpMethodBaseWrapper(new OptionsMethod(uri));
        }

        return null;
    }
    

    private int executeProxyRequest(HttpMethod httpMethodProxyRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        httpMethodProxyRequest.setFollowRedirects(false);

        // Execute the request
        int proxiedResponseCode = client.executeMethod(httpMethodProxyRequest);

        // Check if the proxy response is a redirect
        if (proxiedResponseCode >= HttpServletResponse.SC_MULTIPLE_CHOICES && proxiedResponseCode < HttpServletResponse.SC_NOT_MODIFIED) {
            final String stringStatusCode = Integer.toString(proxiedResponseCode);
            final String stringLocation = httpMethodProxyRequest.getResponseHeader(LOCATION.toString()).getValue();

            if (stringLocation == null) {
                throw new HttpException("Recieved status code: " + stringStatusCode + " but no " + LOCATION.toString() + " header was found in the response");
            }

            httpServletResponse.sendRedirect(stringLocation.replace(proxiedHost.getHostURL(), extractHostPath(httpServletRequest)));
        } else if (proxiedResponseCode == HttpServletResponse.SC_NOT_MODIFIED) {
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            httpServletResponse.setIntHeader(CONTENT_LENGTH.toString(), 0);
            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            // Pass the response code back to the client
            httpServletResponse.setStatus(proxiedResponseCode);

            for (Header header : httpMethodProxyRequest.getResponseHeaders()) {
                httpServletResponse.setHeader(header.getName(), header.getValue());
            }

            // Send the content to the client
            final InputStream source = httpMethodProxyRequest.getResponseBodyAsStream();
            if (source != null) {

              final BufferedInputStream httpIn = new BufferedInputStream(source);
              final OutputStream clientOut = httpServletResponse.getOutputStream();

              //Using a buffered stream so this isn't nearly as expensive as it looks
              int readData;

              while ((readData = httpIn.read()) != -1) {
                  clientOut.write(readData);
              }

              clientOut.flush();
            }
        }

        return proxiedResponseCode;
    }

    private String extractHostPath(HttpServletRequest httpServletRequest) {
        final StringBuilder myHostName = new StringBuilder(httpServletRequest.getServerName());

        if (httpServletRequest.getServerPort() != 80) {
            myHostName.append(":").append(httpServletRequest.getServerPort());
        }

        return myHostName.append(httpServletRequest.getContextPath()).toString();
    }

}

// Utility classes
interface ProcessableRequest {
    public HttpMethod process(RequestProcessor processor) throws IOException;
}

/**
 * Entity Enclosing Requests may send a request body.
 */
class EntityEnclosingMethodWrapper implements ProcessableRequest {
    private final EntityEnclosingMethod method;

    public EntityEnclosingMethodWrapper(EntityEnclosingMethod method) {
      this.method = method;
    }

    @Override
    public HttpMethod process(RequestProcessor processor) throws IOException {
      return processor.process(method);
    }
}

class HttpMethodBaseWrapper implements ProcessableRequest {
    private final HttpMethodBase method;

    public HttpMethodBaseWrapper(HttpMethodBase method) {
      this.method = method;
    }

    @Override
    public HttpMethod process(RequestProcessor processor) throws IOException {
      return processor.process(method);
    }

}

class RequestProcessor {
    private final HttpServletRequest request;
    private final HostConfiguration host;

    public RequestProcessor(HttpServletRequest request, HostConfiguration host) {
      this.request = request;
      this.host = host;
    }

    private void setRequestParameters(HttpMethodBase method) {
      List<NameValuePair> pairs = new ArrayList<NameValuePair>();
      Enumeration<String> names = request.getParameterNames();

      while (names.hasMoreElements()) {
        String name = names.nextElement();
        String[] values = request.getParameterValues(name);
        for (String value: values) {
          pairs.add(new NameValuePair(name, value));
        }
      }

      if (!pairs.isEmpty()) {
        method.setQueryString(pairs.toArray(new NameValuePair[0]));
      }
    }

    private void setHeaders(HttpMethod method) {
        final Enumeration<String> headerNameEnumeration = request.getHeaderNames();

        while (headerNameEnumeration.hasMoreElements()) {
            String stringHeaderName = headerNameEnumeration.nextElement();

            if (stringHeaderName.equalsIgnoreCase(CONTENT_LENGTH.toString())) {
                continue;
            }

            final Enumeration headerValueEnumeration = request.getHeaders(stringHeaderName);

            while (headerValueEnumeration.hasMoreElements()) {
                String stringHeaderValue = (String) headerValueEnumeration.nextElement();

                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                if (stringHeaderName.equalsIgnoreCase(HOST.toString())) {
                    stringHeaderValue = host.getHost() + ":" + host.getPort();
                }

                // Set the same header on the proxy request
                method.setRequestHeader(new Header(stringHeaderName, stringHeaderValue));
            }
        }
    }

    public HttpMethod process(HttpMethodBase method) {
      setHeaders(method);
      setRequestParameters(method);
      return method;

    }

    public HttpMethod process(EntityEnclosingMethod method) throws IOException {
      setHeaders(method);
      setRequestParameters(method);
      method.setRequestEntity(new InputStreamRequestEntity(request.getInputStream()));
      return method;
    }
}
