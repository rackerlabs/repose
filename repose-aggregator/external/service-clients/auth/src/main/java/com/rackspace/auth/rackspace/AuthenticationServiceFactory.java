package com.rackspace.auth.rackspace;

import com.rackspace.auth.AuthServiceException;
import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * @author fran
 */
public class AuthenticationServiceFactory {
   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceFactory.class);

   public AuthenticationService build(String targetHostUri, String username, String password,String connectionPoolId, HttpClientService httpClientService) {
      HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);

      JAXBContext jaxbContext;
      try {
         jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
      } catch (JAXBException e) {
         LOG.error("Problem creating the jaxb context for the Rackspace Auth 1.1 objects.", e);
         throw new AuthServiceException("Possible deployment problem! Unable to build JAXB Context for Auth v1.1 schema types. Reason: "
                 + e.getMessage(), e);
      }

      return new AuthenticationServiceClient(targetHostUri, new ResponseUnmarshaller(jaxbContext), new ServiceClient(targetHostUri,username,password,connectionPoolId,httpClientService));
   }
}
