/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.atomfeed.sax;

// Retrieves admin tokens from os auth for the atom feed reader

import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.auth.openstack.AdminToken;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

public class AdminTokenProvider {

   private String authUrl;
   private String username;
   private String password;
   private ServiceClient client;
   private JAXBContext coreJaxbContext;
   private ResponseUnmarshaller marshaller;
   private AdminToken curAdminToken;
   private final String requestBody;
   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AdminTokenProvider.class);

   public AdminTokenProvider(ServiceClient client, String authUrl, String username, String password) {
      this.client = client;
      this.authUrl = authUrl;
      this.username = username;
      this.password = password;
      setJAXBContext();
      marshaller = new ResponseUnmarshaller(coreJaxbContext);
      ObjectFactory factory = new ObjectFactory();

      PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
      credentials.setUsername(username);
      credentials.setPassword(password);

      JAXBElement jaxbCredentials = factory.createPasswordCredentials(credentials);

      AuthenticationRequest request = new AuthenticationRequest();
      request.setCredential(jaxbCredentials);

      JaxbEntityToXml jaxbToString = new JaxbEntityToXml(coreJaxbContext);
      requestBody = jaxbToString.transform(factory.createAuth(request));
   }

   private void setJAXBContext() {
      try {
         coreJaxbContext = JAXBContext.newInstance(
                 org.openstack.docs.identity.api.v2.ObjectFactory.class,
                 com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class);
      } catch (JAXBException ex) {
      }
   }
   
   public String getFreshAdminToken(){
      
      curAdminToken = null;
      return getAdminToken();
   }

   public String getAdminToken() {

      String adminToken = curAdminToken != null && curAdminToken.isValid() ? curAdminToken.getToken() : null;

      if (adminToken == null) {
         final ServiceClientResponse<AuthenticateResponse> serviceResponse = client.post(authUrl + "/tokens", requestBody, MediaType.APPLICATION_XML_TYPE);

         switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
            case OK:
               final AuthenticateResponse authenticateResponse = marshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);

               Token token = authenticateResponse.getToken();
               curAdminToken = new AdminToken(token.getId(), token.getExpires().toGregorianCalendar());
               adminToken = curAdminToken.getToken();
               break;

            default:
               LOG.error("Unable to get admin token.  Verify admin credentials. " + serviceResponse.getStatusCode());
               curAdminToken = null;
               break;
         }
      }

      return adminToken;
   }
}
