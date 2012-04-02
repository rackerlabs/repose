package com.rackspace.auth.v1_1;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import com.sun.ws.rs.ext.RuntimeDelegateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.RuntimeDelegate;

/**
 * @author fran
 */
public class ServiceClient {
   private static final Logger LOG = LoggerFactory.getLogger(ServiceClient.class);

   static {
      // If this works we need to figure out why and make sure it's part of our init
      RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
   }

   private final Client client;

   public ServiceClient(String username, String password) {
      DefaultClientConfig cc = new DefaultClientConfig();
      cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
      // TODO: Eventually make these values configurable in Repose and implement
      // a "backoff" approach with logging.
      cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 30000);
      cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 30000);
      client = Client.create(cc);

      HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);
      client.addFilter(authFilter);

      if (LOG.isDebugEnabled() || LOG.isTraceEnabled()) {
         LOG.info("Enabling info logging of Rackspace Auth v1.1 client requests");
         client.addFilter(new LoggingFilter());
      }
   }

   public <T> ServiceClientResponse<T> get(String uri, Class<T> entityClass, String... queryParameters) throws AuthServiceException {
      WebResource resource = client.resource(uri);

      if (queryParameters.length % 2 != 0) {
         throw new IllegalArgumentException("Query parameters must be in pairs.");
      }

      for (int index = 0; index < queryParameters.length; index = index + 2) {
         resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
      }

      ClientResponse response = resource.header("Accept", "application/xml").get(ClientResponse.class);
      return new ServiceClientResponse(response.getStatus(), response.getEntity(entityClass));
   }

   public ServiceClientResponse get(String uri, String... queryParameters) throws AuthServiceException {
      WebResource resource = client.resource(uri);

      if (queryParameters.length % 2 != 0) {
         throw new IllegalArgumentException("Query parameters must be in pairs.");
      }

      for (int index = 0; index < queryParameters.length; index = index + 2) {
         resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
      }

      ClientResponse response = resource.header("Accept", "application/xml").get(ClientResponse.class);
      return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
   }

   public ClientResponse getClientResponse(String uri, String... queryParameters) throws AuthServiceException {
      WebResource resource = client.resource(uri);

      if (queryParameters.length % 2 != 0) {
         throw new IllegalArgumentException("Query parameters must be in pairs.");
      }

      for (int index = 0; index < queryParameters.length; index = index + 2) {
         resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
      }

      ClientResponse response = resource.header("Accept", "application/xml").get(ClientResponse.class);
      return response;
   }

}
