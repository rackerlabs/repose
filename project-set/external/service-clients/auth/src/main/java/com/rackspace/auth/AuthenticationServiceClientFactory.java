package com.rackspace.auth;

import com.rackspace.auth.rackspace.AuthenticationService;
import com.rackspace.auth.rackspace.AuthenticationServiceClient;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.rackspace.papi.commons.util.http.ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * @author fran
 */
public class AuthenticationServiceClientFactory {
   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClientFactory.class);

   public AuthenticationService buildRSAuthServiceClient(String targetHostUri, String username, String password) {
      HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);

      JAXBContext jaxbContext;
      try {
         jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
      } catch (JAXBException e) {
         LOG.error("Problem creating the jaxb context for the Rackspace Auth 1.1 objects.", e);
         throw new AuthServiceException("Possible deployment problem! Unable to build JAXB Context for Auth v1.1 schema types. Reason: "
                 + e.getMessage(), e);
      }

      return new AuthenticationServiceClient(targetHostUri, new ResponseUnmarshaller(jaxbContext), new ServiceClient(authFilter));
   }

   public com.rackspace.auth.openstack.AuthenticationService buildOSAuthServiceClient(String targetHostUri, String username, String password) {

      JAXBContext coreJaxbContext;
      JAXBContext groupJaxbContext;
      try {
         coreJaxbContext = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.ObjectFactory.class);
         groupJaxbContext = JAXBContext.newInstance(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class);
      } catch (JAXBException e) {
         LOG.error("Problem creating the jaxb context for the OpenStack Auth objects.", e);
         throw new AuthServiceException("Possible deployment problem! Unable to build JAXB Context for OpenStack Auth schema types. Reason: "
                 + e.getMessage(), e);
      }

      return new com.rackspace.auth.openstack.AuthenticationServiceClient(targetHostUri, username, password,
              new ResponseUnmarshaller(coreJaxbContext),
              new ResponseUnmarshaller(groupJaxbContext));
   }
}
