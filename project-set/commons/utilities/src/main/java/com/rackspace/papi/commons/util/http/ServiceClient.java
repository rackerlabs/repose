package com.rackspace.papi.commons.util.http;

import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.util.Map;

/**
 * @author fran
 */
public class ServiceClient {
   private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);
   private static final int TIMEOUT = 30000;

   private final Client client;

   public ServiceClient(HTTPBasicAuthFilter httpBasicAuthFilter) {
      DefaultClientConfig cc = new DefaultClientConfig();
      cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
      cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, TIMEOUT);
      cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, TIMEOUT);
      client = Client.create(cc);

      if (httpBasicAuthFilter != null) {
         client.addFilter(httpBasicAuthFilter);
      }

      if (LOG.isDebugEnabled() || LOG.isTraceEnabled()) {
         LOG.info("Enabling info logging of Rackspace Auth v1.1 client requests");
         client.addFilter(new LoggingFilter());
      }
   }

   private WebResource.Builder setHeaders(WebResource.Builder builder, Map<String, String> headers) {
      for (String key : headers.keySet()) {
         builder = builder.header(key, headers.get(key));
      }

      return builder;
   }

   public ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType) {
      WebResource resource = client.resource(uri);

      ClientResponse response = resource.type(contentType).header("Accept", "application/xml").post(ClientResponse.class, body);

      return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
   }

   public ServiceClientResponse get(String uri, Map<String, String> headers, String... queryParameters) {
      WebResource resource = client.resource(uri);

      if (queryParameters.length % 2 != 0) {
         throw new IllegalArgumentException("Query parameters must be in pairs.");
      }

      for (int index = 0; index < queryParameters.length; index = index + 2) {
         resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
      }

      WebResource.Builder requestBuilder = resource.getRequestBuilder();
      requestBuilder = setHeaders(requestBuilder, headers);
      ClientResponse response = requestBuilder.get(ClientResponse.class);
      return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
   }
}
