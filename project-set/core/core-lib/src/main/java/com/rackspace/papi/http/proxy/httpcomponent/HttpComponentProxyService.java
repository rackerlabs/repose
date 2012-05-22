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
package com.rackspace.papi.http.proxy.httpcomponent;

import com.rackspace.papi.http.proxy.common.ProxyService;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
public class HttpComponentProxyService implements ProxyService {
    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentProxyService.class);

    private final ClientConnectionManager manager;
    private final HttpClient client;
    private final HttpHost proxiedHost;

    public HttpComponentProxyService(String targetHost) {
        HttpHost host = null;
        try {
            host = URIUtils.extractHost(new URI(targetHost));
        } catch (URISyntaxException ex) {
            LOG.error("Invalid target host url: " + targetHost, ex);
        }

        proxiedHost = host;
        manager = new ThreadSafeClientConnManager();
        client = new DefaultHttpClient(manager);
    }

    @Override
    public int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String target = proxiedHost.toURI() + request.getRequestURI();
        final HttpComponentRequestProcessor processor = new HttpComponentRequestProcessor(request, proxiedHost);
        final HttpComponentProcessableRequest method = HttpComponentFactory.getMethod(request.getMethod(), target);

        if (method != null) {
            HttpRequestBase processedMethod = method.process(processor);

            try {
              return executeProxyRequest(processedMethod, request, response);
            } catch (HttpException ex) {
              LOG.error("Error processing request", ex);
            }
        }

        //Something exploded; return a status code that doesn't exist
        return -1;
    }

    private String extractHostPath(HttpServletRequest request) {
        final StringBuilder myHostName = new StringBuilder(request.getServerName());

        if (request.getServerPort() != 80) {
            myHostName.append(":").append(request.getServerPort());
        }

        return myHostName.append(request.getContextPath()).toString();
    }

    private int executeProxyRequest(HttpRequestBase httpMethodProxyRequest, HttpServletRequest sourceRequest, HttpServletResponse response) throws IOException, HttpException {
        
        //httpMethodProxyRequest.setFollowRedirects(false);

        HttpResponse httpResponse = client.execute(httpMethodProxyRequest);
        HttpComponentResponseCodeProcessor responseCode = new HttpComponentResponseCodeProcessor(httpResponse.getStatusLine().getStatusCode());
        HttpComponentResponseProcessor responseProcessor = new HttpComponentResponseProcessor(httpResponse, response, responseCode);

        if (responseCode.isRedirect()) {
            responseProcessor.sendTranslatedRedirect(proxiedHost.toURI(), extractHostPath(sourceRequest));
        } else {
            responseProcessor.process();
        }

        return responseCode.getCode();
    }
}


