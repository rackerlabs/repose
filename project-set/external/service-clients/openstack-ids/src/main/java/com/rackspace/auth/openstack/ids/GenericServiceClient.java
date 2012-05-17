package com.rackspace.auth.openstack.ids;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.logging.jersey.LoggingFilter;

import javax.xml.bind.JAXBElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fran
 */
public class GenericServiceClient {
   private static final Logger LOG = LoggerFactory.getLogger(GenericServiceClient.class);

   private final Client client;

   public GenericServiceClient() {
      DefaultClientConfig cc = new DefaultClientConfig();
      cc.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
      cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 30000);
      cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 30000);
      client = Client.create(cc);

      LOG.info("Enabling info logging of OpenStack Identity Service client requests");
      client.addFilter(new LoggingFilter());
   }

   public ServiceClientResponse post(String uri, JAXBElement body, MediaType contentType) throws AuthServiceException {
      WebResource resource = client.resource(uri);

      ClientResponse response = resource.type(contentType).header("Accept", "application/xml").post(ClientResponse.class, body);

      return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
   }

   public ServiceClientResponse get(String uri, String adminToken, String... queryParameters) throws AuthServiceException {
      WebResource resource = client.resource(uri);

      if (queryParameters.length % 2 != 0) {
         throw new IllegalArgumentException("Query parameters must be in pairs.");
      }

      for (int index = 0; index < queryParameters.length; index = index + 2) {
         resource = resource.queryParam(queryParameters[index], queryParameters[index + 1]);
      }

      ClientResponse response = resource.header("Accept", "application/xml").header("X-Auth-Token", adminToken).get(ClientResponse.class);
      return new ServiceClientResponse(response.getStatus(), response.getEntityInputStream());
   }
}
