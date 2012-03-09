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
package com.rackspace.cloud.valve.http.proxy.jerseyclient;

import com.rackspace.cloud.valve.http.proxy.jerseyclient.logging.LoggingFilter;
import java.net.MalformedURLException;

import com.rackspace.cloud.valve.http.proxy.common.ProxyService;
//import com.sun.jersey.api.client.filter.LoggingFilter;
import org.apache.http.HttpException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.rackspace.cloud.valve.http.proxy.httpclient.HttpResponseCodeProcessor;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 *
 * @author John Hopper
 */
public class JerseyClientProxyService implements ProxyService {
    private static final String HTTP = "http";
    private static final Logger LOG = LoggerFactory.getLogger(JerseyClientProxyService.class);
    
    private static final Map<String, WebResource> resourceMap = new HashMap<String, WebResource>();

    private final Client client;
    private final URI proxiedHost;
    private final String proxiedHostUrl;

    public JerseyClientProxyService(String targetHost) {
        URI targetUri = null;
        
        try {
          targetUri = new URI(targetHost);
        } catch (URISyntaxException e) {
            LOG.error("Invalid target host url: " + targetHost, e);
        }
        
        proxiedHost = targetUri;
        proxiedHostUrl = asUri(proxiedHost);

        DefaultClientConfig cc = new DefaultClientConfig();
        cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
        cc.getProperties().put(ClientConfig.PROPERTY_THREADPOOL_SIZE, new Integer(20));
        client = Client.create(cc);

        client.addFilter(new LoggingFilter());
    }
    
    private String asUri(URI host) {
      try {
        return new URL(HTTP, host.getHost(), host.getPort(), "").toExternalForm();
      } catch (MalformedURLException ex) {
        LOG.error("Invalid host url: " + host, ex);
      }
      return "";
    }
    
    private WebResource getResource(JerseyRequestProcessor processor, String target) {
      synchronized (resourceMap) {
        WebResource resource = resourceMap.get(target);
        
        if (resource == null) {
          resource = processor.setRequestParameters(client.resource(target));
          resourceMap.put(target, resource);
        }
        
        return resource;
      }
    }
    
    @Override
    public int proxyRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String target = proxiedHostUrl + request.getRequestURI();
        
        JerseyRequestProcessor processor = new JerseyRequestProcessor(request, proxiedHost);
        WebResource resource = client.resource(target);
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


