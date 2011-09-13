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

import com.rackspace.cloud.valve.http.proxy.ProxyService;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import java.io.BufferedInputStream;
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

    private final HttpConnectionManager manager;
    private final HttpClient client;
    private final HostConfiguration proxiedHost;

    public HttpClientProxyService(String targetHost) {
        proxiedHost = new HostConfiguration();
        try {
            proxiedHost.setHost(new URI(targetHost, false));
        } catch (URIException e) {
            e.printStackTrace();
        }

        manager = new MultiThreadedHttpConnectionManager();
        client = new HttpClient(manager);
    }

    @Override
    public int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String target = proxiedHost.getHostURL() + request.getRequestURI();
        final HttpMethod method = newMethod(request.getMethod(), target);

        if (method != null) {
            //Replicate the meaningful headers           
            setProxyRequestHeaders(request, method);
            
            return executeProxyRequest(method, request, response);
        }

        //Something exploded; return a status code that doesn't exist
        return -1;
    }

    private HttpMethod newMethod(String method, String uri) {
        if (method.equalsIgnoreCase("GET")) {
            return new GetMethod(uri);
        }

        if (method.equalsIgnoreCase("PUT")) {
            return new PutMethod(uri);
        }

        if (method.equalsIgnoreCase("POST")) {
            return new PostMethod(uri);
        }

        if (method.equalsIgnoreCase("DELETE")) {
            return new DeleteMethod(uri);
        }

        if (method.equalsIgnoreCase("HEAD")) {
            return new HeadMethod(uri);
        }

        if (method.equalsIgnoreCase("OPTIONS")) {
            return new OptionsMethod(uri);
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
            final BufferedInputStream httpIn = new BufferedInputStream(httpMethodProxyRequest.getResponseBodyAsStream());
            final OutputStream clientOut = httpServletResponse.getOutputStream();

            //Using a buffered stream so this isn't nearly as expensive as it looks
            int intNextByte;

            while ((intNextByte = httpIn.read()) != -1) {
                clientOut.write(intNextByte);
            }

            clientOut.flush();
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

    private void setProxyRequestHeaders(HttpServletRequest httpServletRequest, HttpMethod httpMethodProxyRequest) {
        final Enumeration headerNameEnumeration = httpServletRequest.getHeaderNames();

        while (headerNameEnumeration.hasMoreElements()) {
            String stringHeaderName = (String) headerNameEnumeration.nextElement();

            if (stringHeaderName.equalsIgnoreCase(CONTENT_LENGTH.toString())) {
                continue;
            }

            final Enumeration headerValueEnumeration = httpServletRequest.getHeaders(stringHeaderName);

            while (headerValueEnumeration.hasMoreElements()) {
                String stringHeaderValue = (String) headerValueEnumeration.nextElement();

                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                if (stringHeaderName.equalsIgnoreCase(HOST.toString())) {
                    stringHeaderValue = proxiedHost.getHost() + ":" + proxiedHost.getPort();
                }

                // Set the same header on the proxy request
                httpMethodProxyRequest.setRequestHeader(new Header(stringHeaderName, stringHeaderValue));
            }
        }
    }
}
