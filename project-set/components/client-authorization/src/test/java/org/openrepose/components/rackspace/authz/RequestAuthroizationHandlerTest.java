package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.openrepose.components.authz.rackspace.config.ServiceEndpoint;
import org.openstack.docs.identity.api.v2.Endpoint;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RequestAuthroizationHandlerTest {

   public static class WhenAuthorizingRequests {

      private static final String UNAUTHORIZED_TOKEN = "abcdef-abcdef-abcdef-abcdef";
      private static final String AUTHORIZED_TOKEN = "authorized";
      private static final String PUBLIC_URL = "http://service.api.f.com/v1.1";
      
      private HttpServletRequest mockedRequest;
      private RequestAuthroizationHandler handler;

      @Before
      public void standUp() {
         final List<Endpoint> endpointList = new LinkedList<Endpoint>();
         
         Endpoint endpoint = new Endpoint();
         endpoint.setPublicURL(PUBLIC_URL);
         
         endpointList.add(endpoint);
         
         final OpenStackAuthenticationService mockedAuthService = mock(OpenStackAuthenticationService.class);
         when(mockedAuthService.getEndpointsForToken(UNAUTHORIZED_TOKEN)).thenReturn(Collections.EMPTY_LIST);
         when(mockedAuthService.getEndpointsForToken(AUTHORIZED_TOKEN)).thenReturn(endpointList);
         
         final ServiceEndpoint myServiceEndpoint = new ServiceEndpoint();
         myServiceEndpoint.setHref(PUBLIC_URL);
         
         handler = new RequestAuthroizationHandler(mockedAuthService, myServiceEndpoint);

         mockedRequest = mock(HttpServletRequest.class);
      }

      @Test
      public void shouldRejectDelegatedAuthentication() {
         when(mockedRequest.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");

         final FilterDirector director = handler.handleRequest(mockedRequest, null);

         assertEquals("Authorization component must return requests that have had authentication delegated", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Authorization component must reject delegated authentication with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
      }

      @Test
      public void shouldRejectRequestsWithoutAuthTokens() {
         final FilterDirector director = handler.handleRequest(mockedRequest, null);

         assertEquals("Authorization component must return requests that do not have auth tokens", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Authorization component must reject unauthenticated requests with a 401", HttpStatusCode.UNAUTHORIZED, director.getResponseStatus());
      }

      @Test
      public void shouldRejectUnauthorizedRequests() {
         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(UNAUTHORIZED_TOKEN);

         final FilterDirector director = handler.handleRequest(mockedRequest, null);

         assertEquals("Authorization component must return unauthorized requests", FilterAction.RETURN, director.getFilterAction());
         assertEquals("Authorization component must reject unauthorized requests with a 403", HttpStatusCode.FORBIDDEN, director.getResponseStatus());
      }

      @Test
      public void shouldPassAuthorizedRequests() {
         when(mockedRequest.getHeader(CommonHttpHeader.AUTH_TOKEN.toString())).thenReturn(AUTHORIZED_TOKEN);

         final FilterDirector director = handler.handleRequest(mockedRequest, null);

         assertEquals("Authorization component must pass authorized requests", FilterAction.PASS, director.getFilterAction());
      }
   }
}
