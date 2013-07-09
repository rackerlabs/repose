package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthToken;
import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.io.FilePathReaderImpl;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class AuthenticationServiceClientTest {

   public static class TestParent {

      AuthenticationServiceClient authenticationServiceClient;
      ResponseUnmarshaller responseUnmarshaller, coreUnmarshaller;
      ServiceClientResponse<AuthenticateResponse> serviceClientResponseGet, serviceClientResponsePost;
      ServiceClient serviceClient;
      String tenant;
      String userToken;
      String targetHostUri;
      String username;
      String password;
      String tenantId;
      JAXBContext coreJaxbContext;

      @Before
      public void setUp() throws Exception {

         coreJaxbContext = JAXBContext.newInstance(
                 org.openstack.docs.identity.api.v2.ObjectFactory.class,
                 com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class);
         AppenderForTesting.clear();
         tenant = "tenant";
         userToken = "userToken";
         targetHostUri = "targetHostUri";
         username = "username";
         password = "password";
         tenantId = "tenantId";
         coreUnmarshaller = new ResponseUnmarshaller(coreJaxbContext);
         responseUnmarshaller = mock(ResponseUnmarshaller.class);
         serviceClientResponseGet = mock(ServiceClientResponse.class);
         serviceClientResponsePost = mock(ServiceClientResponse.class);
         serviceClient = mock(ServiceClient.class);
         authenticationServiceClient =
                 new AuthenticationServiceClient(targetHostUri, username, password, tenantId, coreUnmarshaller,
                 responseUnmarshaller, serviceClient);
      }

      @After
      public void tearDown() throws Exception {
         AppenderForTesting.clear();
      }

      @Test
      public void shouldErrorWithCorrectMessageForInternalServerErrorCase() {
         when(serviceClient.get(anyString(), any(Map.class), anyString(), anyString()))
                 .thenReturn(serviceClientResponseGet);
         when(serviceClient.post(anyString(), any(JAXBElement.class), any(MediaType.class)))
                 .thenReturn(serviceClientResponsePost);
         when(serviceClientResponseGet.getStatusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
         when(serviceClientResponsePost.getStatusCode()).thenReturn(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());

         authenticationServiceClient.validateToken(tenant, userToken);

         assertTrue(AppenderForTesting.getMessages()[1]
                 .startsWith("Authentication Service returned internal server error:"));
      }

      @Test
      public void shouldErrorWithCorrectMessageForDefaultErrorCase() {
         when(serviceClient.get(anyString(), any(Map.class), anyString(), anyString()))
                 .thenReturn(serviceClientResponseGet);
         when(serviceClient.post(anyString(), any(JAXBElement.class), any(MediaType.class)))
                 .thenReturn(serviceClientResponsePost);
         when(serviceClientResponseGet.getStatusCode()).thenReturn(999);
         when(serviceClientResponsePost.getStatusCode()).thenReturn(999);

         authenticationServiceClient.validateToken(tenant, userToken);

         assertTrue(AppenderForTesting.getMessages()[1]
                 .startsWith("Authentication Service returned an unexpected response status code:"));
      }

      @Test
      public void ShouldReturnNullTokenOn404() throws JAXBException, IOException {

         ServiceClientResponse<AuthenticateResponse> resp = getServiceClientResponse();
         ServiceClientResponse getResponse = new ServiceClientResponse(404, null);
         when(serviceClient.get(anyString(), anyMap())).thenReturn(getResponse);
         when(serviceClient.post(anyString(), any(JAXBElement.class), eq(MediaType.APPLICATION_XML_TYPE))).thenReturn(resp);
         AuthToken token = authenticationServiceClient.validateToken("", userToken);
         assertEquals("Should contain Default Region", token, null);
      }

      @Test
      public void shouldReturnDefaultRegionFromTokenValidation() throws JAXBException, IOException {
         
         ServiceClientResponse<AuthenticateResponse> resp = getServiceClientResponse();
         FilePathReaderImpl fileReader1 = new FilePathReaderImpl(File.separator + "META-INF" + File.separator + "ValidateTokenResponse.xml");
         ServiceClientResponse getResponse = new ServiceClientResponse(200, fileReader1.getResourceAsStream());
         when(serviceClient.get(anyString(), anyMap())).thenReturn(getResponse);
         when(serviceClient.post(anyString(), any(JAXBElement.class), eq(MediaType.APPLICATION_XML_TYPE))).thenReturn(resp);

         AuthToken token = authenticationServiceClient.validateToken("", userToken);

         assertEquals("Should contain Default Region", token.getDefaultRegion(), "SAT");
      }
      
      @Test
      public void shouldReturnNoRegionFromTokenValidation() throws JAXBException, IOException {
         
         ServiceClientResponse<AuthenticateResponse> resp = getServiceClientResponse();
         FilePathReaderImpl fileReader1 = new FilePathReaderImpl(File.separator + "META-INF" + File.separator + "ValidateTokenResponseNoRegion.xml");
         ServiceClientResponse getResponse = new ServiceClientResponse(200, fileReader1.getResourceAsStream());
         when(serviceClient.get(anyString(), anyMap())).thenReturn(getResponse);
         when(serviceClient.post(anyString(), any(JAXBElement.class), eq(MediaType.APPLICATION_XML_TYPE))).thenReturn(resp);

         AuthToken token = authenticationServiceClient.validateToken("", userToken);

         assertEquals("Should contain Default Region", token.getDefaultRegion(), "");
      }

      private ServiceClientResponse getServiceClientResponse() throws JAXBException, IOException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         AuthenticateResponse response = getServiceResponse();
         ObjectFactory factory = new ObjectFactory();
         Marshaller marshaller = coreJaxbContext.createMarshaller();
         marshaller.marshal(factory.createAccess(response), baos);

         baos.flush();
         baos.close();

         InputStream is = new ByteArrayInputStream(baos.toByteArray());
         ServiceClientResponse<AuthenticateResponse> resp = new ServiceClientResponse<AuthenticateResponse>(200, is);

         return resp;
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
}
