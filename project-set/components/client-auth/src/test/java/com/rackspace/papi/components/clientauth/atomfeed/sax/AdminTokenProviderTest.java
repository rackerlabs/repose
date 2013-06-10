/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.atomfeed.sax;

import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;
public class AdminTokenProviderTest {

   ServiceClient client;
   AdminTokenProvider provider;

   @Before
   public void setUp() {

      client = mock(ServiceClient.class);


   }

   @Test
   public void shouldRetrieveAdminToken() throws IOException, JAXBException {
      
      
      JAXBContext coreJaxbContext = JAXBContext.newInstance(
                 org.openstack.docs.identity.api.v2.ObjectFactory.class,
                 com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      AuthenticateResponse response = getServiceResponse();
      ObjectFactory factory = new ObjectFactory();
      Marshaller marshaller = coreJaxbContext.createMarshaller();
      marshaller.marshal(factory.createAccess(response), baos);

      baos.flush();
      baos.close();
      
      InputStream is = new ByteArrayInputStream(baos.toByteArray());
      ServiceClientResponse<AuthenticateResponse> resp = new ServiceClientResponse<AuthenticateResponse>(200, is);
      when(client.post(anyString(), any(JAXBElement.class), eq(MediaType.APPLICATION_XML_TYPE))).thenReturn(resp);
      provider = new AdminTokenProvider(client, "authUrl", "user", "pass");

      String adminToken = provider.getAdminToken();
      assertTrue(adminToken.equals("tokenid"));
   }

   private static AuthenticateResponse getServiceResponse() {
      AuthenticateResponse rsp = new AuthenticateResponse();

      Token token = new Token();
      token.setId("tokenid");
      GregorianCalendar cal = new GregorianCalendar(2013, 11, 12);
      token.setExpires(new XMLGregorianCalendarImpl(cal));
      TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
      tenantForAuthenticateResponse.setId("tenantId");
      tenantForAuthenticateResponse.setName("tenantName");
      token.setTenant(tenantForAuthenticateResponse);
      rsp.setToken(token);

      ServiceCatalog catalog = new ServiceCatalog();
      List<ServiceForCatalog> serviceCatalogList = new ArrayList<ServiceForCatalog>();
      ServiceForCatalog serviceForCatalog = new ServiceForCatalog();
      serviceForCatalog.setName("catName");
      serviceForCatalog.setType("type");
      serviceCatalogList.add(serviceForCatalog);
      catalog.getService().addAll(serviceCatalogList);

      rsp.setServiceCatalog(catalog);

      UserForAuthenticateResponse user = new UserForAuthenticateResponse();
      user.setId("userId");
      user.setName("userName");
      RoleList roles = new RoleList();

      Role role = new Role();
      role.setDescription("role description");
      role.setId("roleId");
      role.setName("roleName");
      role.setServiceId("serviceId");
      role.setTenantId("roleTenantId");
      roles.getRole().add(role);

      user.setRoles(roles);

      rsp.setUser(user);

      return rsp;
   }
}