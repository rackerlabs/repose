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
package com.rackspace.papi.http.proxy.httpclient;

import com.rackspace.papi.http.proxy.common.HttpResponseCodeProcessor;
import com.rackspace.papi.http.proxy.common.ProxyService;
import org.apache.commons.httpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
      final HttpRequestProcessor processor = new HttpRequestProcessor(request, proxiedHost);
      final ProcessableRequest method = HttpMethodFactory.getMethod(request.getMethod(), target);

      if (method != null) {
         HttpMethod processedMethod = method.process(processor);
         try {
            return executeProxyRequest(processedMethod, request, response);
         } catch (org.apache.http.HttpException ex) {
            LOG.error("Error executing proxied request", ex);
            return -1;
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

   private int executeProxyRequest(HttpMethod httpMethodProxyRequest, HttpServletRequest sourceRequest, HttpServletResponse response) throws IOException, org.apache.http.HttpException {

      httpMethodProxyRequest.setFollowRedirects(false);

      HttpResponseCodeProcessor responseCode = new HttpResponseCodeProcessor(client.executeMethod(httpMethodProxyRequest));
      HttpResponseProcessor responseProcessor = new HttpResponseProcessor(httpMethodProxyRequest, response, responseCode);

      if (responseCode.isRedirect()) {
         responseProcessor.sendTranslatedRedirect(proxiedHost.getHostURL(), extractHostPath(sourceRequest));
      } else {
         responseProcessor.process();
      }

      return responseCode.getCode();
   }
}
