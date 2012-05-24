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
package com.rackspace.papi.http.proxy.jerseyclient;

import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.http.proxy.common.ProxyService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author John Hopper
 */
public class JerseyClientProxyService implements ProxyService {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyClientProxyService.class);
    private Client client;
    private final URI proxiedHost;
    private final String proxiedHostUrl;
    private final Integer connectionTimeout;
    private final Integer readTimeout;

    public JerseyClientProxyService(String targetHost, Integer connectionTimeout, Integer readTimeout) {
        URI targetUri = null;

        try {
            targetUri = new URI(targetHost);
        } catch (URISyntaxException e) {
            LOG.error("Invalid target host url: " + targetHost, e);
        }

        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;

        proxiedHost = targetUri;
        proxiedHostUrl = asUri(proxiedHost);
    }

    private synchronized Client getClient() {
        if (client == null) {
            DefaultClientConfig cc = new JerseyPropertiesConfigurator(connectionTimeout, readTimeout).configure();
            client = Client.create(cc);

            if (LOG.isInfoEnabled()) {
                LOG.info("Enabling info logging of jersey client requests");
                client.addFilter(new LoggingFilter());
            } else {
                LOG.warn("**** Jersey client request logging not enabled *****");
            }
                
        }

        return client;
    }

    private String asUri(URI host) {
        try {
            return new URL(host.getScheme(), host.getHost(), host.getPort(), "").toExternalForm();
        } catch (MalformedURLException ex) {
            LOG.error("Invalid host url: " + host, ex);
        }
        return "";
    }

    @Override
    public int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String target = proxiedHostUrl + request.getRequestURI();

        JerseyRequestProcessor processor = new JerseyRequestProcessor(request, proxiedHost);
        WebResource resource = getClient().resource(target);
        Builder builder = processor.process(resource);
        try {
            return executeProxyRequest(builder, request, response);
        } catch (HttpException ex) {
            LOG.error("Error processing request", ex);
        }

        return -1;
    }

    private String extractHostPath(HttpServletRequest request) {
        final StringBuilder myHostName = new StringBuilder(request.getServerName());

        if (request.getServerPort() != 80) {
            myHostName.append(":").append(request.getServerPort());
        }

        return myHostName.append(request.getContextPath()).toString();
    }

    private int executeProxyRequest(Builder builder, HttpServletRequest sourceRequest, HttpServletResponse sourceResponse) throws IOException, HttpException {

        ClientResponse response = builder.method(sourceRequest.getMethod(), ClientResponse.class);

        HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(response.getStatus());
        JerseyResponseProcessor responseProcessor = new JerseyResponseProcessor(response, sourceResponse);

        if (responseCode.isRedirect()) {
            responseProcessor.sendTranslatedRedirect(proxiedHostUrl, extractHostPath(sourceRequest));
        } else {
            responseProcessor.process();
        }

        return responseCode.getCode();
    }
}
