package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.auth.openstack.ids.CachableUserInfo;
import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientauth.UriMatcher;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import com.rackspace.papi.components.clientauth.openstack.config.OpenStackIdentityService;
import com.rackspace.papi.components.clientauth.openstack.config.OpenstackAuth;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

/**
 * @author zinic
 */
@RunWith(Enclosed.class)
public class OpenStackAuthenticationHandlerTest {

   @Ignore
   public static abstract class TestParent {

      protected HttpServletRequest request;
      protected ReadableHttpServletResponse response;
      protected OpenStackAuthenticationService authService;
      protected OpenStackAuthenticationHandler handler;
      protected OpenStackAuthenticationHandler handlerWithCache;
      protected OpenstackAuth osauthConfig;
      protected KeyedRegexExtractor keyedRegexExtractor;
      protected Datastore store;
      protected List<Pattern> whiteListRegexPatterns;


      @Before
      public void beforeAny() {
         request = mock(HttpServletRequest.class);
         response = mock(ReadableHttpServletResponse.class);

         osauthConfig = new OpenstackAuth();
         osauthConfig.setDelegable(delegable());

         keyedRegexExtractor = new KeyedRegexExtractor();

         final ClientMapping mapping = new ClientMapping();
         mapping.setIdRegex("/start/(.*)/");

         osauthConfig.getClientMapping().add(mapping);
         keyedRegexExtractor.addPattern(mapping.getIdRegex());

         final OpenStackIdentityService openStackIdentityService = new OpenStackIdentityService();
         openStackIdentityService.setUri("http://some.auth.endpoint");
         osauthConfig.setIdentityService(openStackIdentityService);

         authService = mock(OpenStackAuthenticationService.class);

         whiteListRegexPatterns = new ArrayList<Pattern>();
         whiteListRegexPatterns.add(Pattern.compile("/v1.0/application\\.wadl"));

         handler = new OpenStackAuthenticationHandler(osauthConfig, authService, keyedRegexExtractor, null, new UriMatcher(whiteListRegexPatterns));


         // Handler with cache
         store = mock(Datastore.class);
         OpenStackUserInfoCache cache = new OpenStackUserInfoCache(store);

         handlerWithCache = new OpenStackAuthenticationHandler(osauthConfig, authService, keyedRegexExtractor, cache, new UriMatcher(whiteListRegexPatterns));
      }

      protected abstract boolean delegable();

      public CachableUserInfo generateCachableTokenInfo(String roles, String tokenId, String username) {
         return generateCachableTokenInfo(roles, tokenId, username, 10000);
      }

      protected Calendar getCalendarWithOffset(int millis) {
         return getCalendarWithOffset(Calendar.MILLISECOND, millis);
      }

      protected Calendar getCalendarWithOffset(int field, int millis) {
         Calendar cal = GregorianCalendar.getInstance();

         cal.add(field, millis);

         return cal;
      }

      public CachableUserInfo generateCachableTokenInfo(String roles, String tokenId, String username, int ttl) {
         Long expires = getCalendarWithOffset(ttl).getTimeInMillis();

         final CachableUserInfo cti = mock(CachableUserInfo.class);
         when(cti.getRoles()).thenReturn(roles);
         when(cti.getTokenId()).thenReturn(tokenId);
         when(cti.getUsername()).thenReturn(username);
         when(cti.getExpires()).thenReturn(expires);

         return cti;
      }
   }

   public static class WhenCachingUserInfo extends TestParent {
      private DatatypeFactory dataTypeFactory;
      AuthenticateResponse authResponse;

      @Override
      protected boolean delegable() {
         return false;
      }

      @Before
      public void standUp() throws DatatypeConfigurationException {
         dataTypeFactory = DatatypeFactory.newInstance();
         when(request.getRequestURI()).thenReturn("/start/104772/resource");
         when(request.getHeader(anyString())).thenReturn("tokenId");

         Calendar expires = getCalendarWithOffset(1000);

         authResponse = new AuthenticateResponse();
         UserForAuthenticateResponse user = new UserForAuthenticateResponse();
         user.setId("104772");
         user.setName("user2");

         Token token = new Token();
         token.setId("tokenId");
         token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));

         authResponse.setToken(token);
         authResponse.setUser(user);
      }

      @Test
      public void shouldCheckCacheForCredentials() throws IOException {
         final CachableUserInfo userInfo = new CachableUserInfo(null, authResponse);
         byte[] userInfoBytes = ObjectSerializer.instance().writeObject(userInfo);
         when(authService.validateToken(anyString(), anyString())).thenReturn(userInfo);


         final FilterDirector director = handlerWithCache.handleRequest(request, response);

         verify(store).get(eq(OpenStackUserInfoCache.AUTH_TOKEN_CACHE_PREFIX + ".104772"));
         assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldUseCachedUserInfo() {
         final CachableUserInfo userInfo = new CachableUserInfo(null, authResponse);
         StoredElement element = mock(StoredElement.class);
         when(element.elementIsNull()).thenReturn(false);
         when(element.elementAs(CachableUserInfo.class)).thenReturn(userInfo);
         when(authService.validateToken(anyString(), anyString())).thenReturn(userInfo);

         when(store.get(eq(OpenStackUserInfoCache.AUTH_TOKEN_CACHE_PREFIX + ".104772"))).thenReturn(element);

         final FilterDirector director = handlerWithCache.handleRequest(request, response);

         // Service should not be called if we found the token in the cache
         verify(authService, times(0)).validateToken(anyString(), anyString());
         assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldNotUseCachedUserInfoForExpired() throws InterruptedException {
         final CachableUserInfo userInfo = new CachableUserInfo(null, authResponse);
         StoredElement element = mock(StoredElement.class);
         when(element.elementIsNull()).thenReturn(false);
         when(element.elementAs(CachableUserInfo.class)).thenReturn(userInfo);
         when(authService.validateToken(anyString(), anyString())).thenReturn(userInfo);
         when(store.get(eq(OpenStackUserInfoCache.AUTH_TOKEN_CACHE_PREFIX + ".104772"))).thenReturn(element);

         // Wait until token expires
         Thread.sleep(1000);

         final FilterDirector director = handlerWithCache.handleRequest(request, response);

         // Service should be called since token has expired
         verify(authService, times(1)).validateToken(anyString(), anyString());
         assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldNotUseCachedUserInfoForBadTokenId() {
         authResponse.getToken().setId("differentId");
         final CachableUserInfo userInfo = new CachableUserInfo(null, authResponse);
         StoredElement element = mock(StoredElement.class);
         when(element.elementIsNull()).thenReturn(false);
         when(element.elementAs(CachableUserInfo.class)).thenReturn(userInfo);
         when(authService.validateToken(anyString(), anyString())).thenReturn(userInfo);

         when(store.get(eq(OpenStackUserInfoCache.AUTH_TOKEN_CACHE_PREFIX + ".104772"))).thenReturn(element);

         final FilterDirector director = handlerWithCache.handleRequest(request, response);

         verify(authService, times(1)).validateToken(anyString(), anyString());
         assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
      }
   }

   public static class WhenAuthenticatingDelegatableRequests extends TestParent {

      @Override
      protected boolean delegable() {
         return true;
      }

      @Before
      public void standUp() {
         when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
      }

      @Test
      public void shouldPassNullOrBlankCredentials() {
         when(request.getRequestURI()).thenReturn("/start/");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must pass requests with invalid credentials", FilterAction.PROCESS_RESPONSE, requestDirector.getFilterAction());
      }

      @Test
      public void shouldRejectInvalidCredentials() {
         when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must reject requests with invalid credentials", FilterAction.RETURN, requestDirector.getFilterAction());
      }
   }

   public static class WhenAuthenticatingNonDelegatableRequests extends TestParent {

      @Override
      protected boolean delegable() {
         return false;
      }

      @Before
      public void standUp() {
         when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
         when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
      }

      @Test
      public void shouldPassValidCredentials() {
         final CachableUserInfo tokenInfo = generateCachableTokenInfo("", "", "");
         when(authService.validateToken(anyString(), anyString())).thenReturn(tokenInfo);

         final FilterDirector director = handler.handleRequest(request, response);

         assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
      }

      @Test
      public void shouldRejectInvalidCredentials() {
         final FilterDirector director = handler.handleRequest(request, response);

         assertEquals("Auth component must reject invalid requests", FilterAction.RETURN, director.getFilterAction());
      }
   }

   public static class WhenHandlingResponseFromServiceInDelegatedMode extends TestParent {
      @Override
      protected boolean delegable() {
         return true;
      }

      @Test
      public void shouldNotModifyResponseOnResponseStatusCodeNotEqualTo401or403() {
         when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
         when(response.getStatus()).thenReturn(200);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         assertEquals("Auth component must pass valid, delegated responses", FilterAction.NOT_SET, responseDirector.getFilterAction());
      }

      @Test
      public void shouldModifyDelegatedWwwAuthenticateHeaderOn401() {
         when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
         when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
         when(response.getStatus()).thenReturn(401);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         final String expected = "Keystone uri=" + osauthConfig.getIdentityService().getUri();

         assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
      }

      @Test
      public void shouldModifyDelegatedWwwAuthenticateHeaderOn403() {
         when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
         when(response.getStatus()).thenReturn(403);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         final String expected = "Keystone uri=" + osauthConfig.getIdentityService().getUri();

         assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(CommonHttpHeader.WWW_AUTHENTICATE.toString()).iterator().next());
      }

      @Test
      public void shouldReturn500OnAuth501FailureWithDelegatedWwwAuthenticateHeaderSet() {
         when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
         when(response.getStatus()).thenReturn(501);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
      }
   }

   public static class WhenHandlingResponseFromServiceNotInDelegatedMode extends TestParent {
      @Override
      protected boolean delegable() {
         return false;
      }

      @Test
      public void shouldReturn501OnAuthFailureWithNonDelegatedWwwAuthenticateHeaderSet() {
         when(response.getStatus()).thenReturn(401);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
      }

      @Test
      public void shouldReturn501OnAuthFailureWithNoWwwAuthenticateHeaderSet() {
         when(response.getStatus()).thenReturn(401);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.INTERNAL_SERVER_ERROR, responseDirector.getResponseStatus());
      }

      @Test
      public void shouldReturn501OnAuth501FailureWithDelegatedWwwAuthenticateHeaderNotSet() {
         when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Not-Delegate");
         when(response.getStatus()).thenReturn(501);

         final FilterDirector responseDirector = handler.handleResponse(request, response);

         assertEquals("Auth component must identify proxy auth failures", HttpStatusCode.NOT_IMPLEMENTED, responseDirector.getResponseStatus());
      }
   }

   public static class WhenHandlingWhiteListNotInDelegatedMode extends TestParent {

      @Override
      protected boolean delegable() {
         return false;
      }

      @Test
      public void shouldPassUriOnWhiteList() {
         when(request.getRequestURI()).thenReturn("/v1.0/application.wadl");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must pass requests with uri on white list", FilterAction.PASS, requestDirector.getFilterAction());
      }

      @Test
      public void shouldReturnForUriNotOnWhiteList() {
         when(request.getRequestURI()).thenReturn("?param=/v1.0/application.wadl");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must return requests with uri not on white list", FilterAction.RETURN, requestDirector.getFilterAction());
      }
   }

   public static class WhenHandlingWhiteListInDelegatedMode extends TestParent {

      @Override
      protected boolean delegable() {
         return true;
      }

      @Test
      public void shouldPassUriOnWhiteList() {
         when(request.getRequestURI()).thenReturn("/v1.0/application.wadl");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must pass requests with uri on white list", FilterAction.PASS, requestDirector.getFilterAction());
      }

      @Test
      public void shouldProcessUriNotOnWhiteListAsNonAuthedRequest() {
         when(request.getRequestURI()).thenReturn("?param=/v1.0/application.wadl");
         final FilterDirector requestDirector = handler.handleRequest(request, response);
         assertEquals("Auth component must process requests with uri not on white list when in delegated mode", FilterAction.PROCESS_RESPONSE, requestDirector.getFilterAction());
      }
   }
}
