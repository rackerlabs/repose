/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.clientauth.atomfeed.sax;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public class AdminTokenProviderTest {

   AkkaServiceClient client;
   AdminTokenProvider provider;

   @Before
   public void setUp() {
      client = mock(AkkaServiceClient.class);
   }

   @Test
   public void shouldRetrieveAdminToken() throws Exception {
      
      
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
      ServiceClientResponse resp = new ServiceClientResponse(200, is);
      when(client.post(anyString(),anyString(),  anyMapOf(String.class, String.class), anyString(), eq(MediaType.APPLICATION_XML_TYPE))).thenReturn(resp);
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
      List<ServiceForCatalog> serviceCatalogList = new ArrayList<>();
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
