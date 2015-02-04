package org.openrepose.filters.clientauth.openstack;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthGroups;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.common.auth.openstack.AuthenticationService;
import org.openrepose.common.auth.openstack.OpenStackGroup;
import org.openrepose.common.auth.openstack.OpenStackToken;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.io.ObjectSerializer;
import org.openrepose.commons.utils.regex.KeyedRegexExtractor;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.clientauth.common.*;
import org.openrepose.filters.clientauth.openstack.config.ClientMapping;
import org.openrepose.filters.clientauth.openstack.config.OpenStackIdentityService;
import org.openrepose.filters.clientauth.openstack.config.OpenstackAuth;
import org.openrepose.filters.clientauth.openstack.config.ServiceAdminRoles;
import org.openrepose.services.datastore.Datastore;
import org.openstack.docs.identity.api.v2.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import java.util.*;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;




/**
 * @author zinic
 */
@RunWith(Enclosed.class)
public class OpenStackAuthenticationHandlerTest {

    @Ignore
    public static abstract class TestParent {
        protected static final String AUTH_TOKEN_CACHE_PREFIX = "openstack.identity.token";
        protected static final String AUTH_GROUP_CACHE_PREFIX = "openstack.identity.group";   
        protected static final String AUTH_USER_CACHE_PREFIX = "openstack.identity.user";  
        protected static final long AUTH_GROUP_CACHE_TTL = 600000;
        protected static final long AUTH_TOKEN_CACHE_TTL = 5000;
        protected static final long AUTH_USER_CACHE_TTL = 5000;
        protected static final int AUTH_CACHE_OFFSET = 0;
        protected static final String ENDPOINTS_CACHE_PREFIX = "openstack.endpoints.cache";

        protected HttpServletRequest request;
        protected ReadableHttpServletResponse response;
        protected AuthenticationService authService;
        protected OpenStackAuthenticationHandler handler;
        protected OpenStackAuthenticationHandler handlerWithCache;
        protected OpenstackAuth osauthConfig;
        protected KeyedRegexExtractor keyedRegexExtractor;
        protected Datastore store;
        protected List<Pattern> whiteListRegexPatterns;
        protected EndpointsConfiguration endpointsConfiguration;

        @Before
        public void beforeAny() {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);

            osauthConfig = new OpenstackAuth();
            osauthConfig.setTenanted(isTenanted());

            keyedRegexExtractor = new KeyedRegexExtractor();

            final ClientMapping mapping = new ClientMapping();
            mapping.setIdRegex("/start/([^/]*)/");

            final ClientMapping mapping2 = new ClientMapping();
            mapping2.setIdRegex(".*\\?.*username=(.+)");

            osauthConfig.getClientMapping().add(mapping);
            osauthConfig.getClientMapping().add(mapping2);
            keyedRegexExtractor.addPattern(mapping.getIdRegex());
            keyedRegexExtractor.addPattern(mapping2.getIdRegex());

            final OpenStackIdentityService openStackIdentityService = new OpenStackIdentityService();
            openStackIdentityService.setUri("http://some.auth.endpoint");
            osauthConfig.setIdentityService(openStackIdentityService);

            authService = mock(AuthenticationService.class);

            whiteListRegexPatterns = new ArrayList<Pattern>();
            whiteListRegexPatterns.add(Pattern.compile("/v1.0/application\\.wadl"));

            final ServiceAdminRoles serviceAdminRoles = new ServiceAdminRoles();
            serviceAdminRoles.getRole().add("12345");

            endpointsConfiguration = new EndpointsConfiguration("json", AUTH_USER_CACHE_TTL, new Integer("1000"));
            Configurables configurables = new Configurables(
                    delegable(),
                    0.7,
                    "http://some.auth.endpoint",
                    keyedRegexExtractor,
                    isTenanted(),
                    AUTH_GROUP_CACHE_TTL,
                    AUTH_TOKEN_CACHE_TTL,
                    AUTH_USER_CACHE_TTL,
                    AUTH_CACHE_OFFSET,
                    requestGroups(),
                    endpointsConfiguration,
                    serviceAdminRoles.getRole(),
                    new ArrayList<String>(), false, false);
            handler = new OpenStackAuthenticationHandler(configurables, authService, null, null,null,null, new UriMatcher(whiteListRegexPatterns));


            // Handler with cache
            store = mock(Datastore.class);
            AuthTokenCache cache = new AuthTokenCache(store, AUTH_TOKEN_CACHE_PREFIX);
            AuthGroupCache grpCache = new AuthGroupCache(store, AUTH_GROUP_CACHE_PREFIX);
            AuthUserCache usrCache = new AuthUserCache(store, AUTH_USER_CACHE_PREFIX);
            EndpointsCache endpointsCache = new EndpointsCache(store, ENDPOINTS_CACHE_PREFIX);

            handlerWithCache = new OpenStackAuthenticationHandler(configurables, authService, cache, grpCache,usrCache, endpointsCache, new UriMatcher(whiteListRegexPatterns));
        }

        protected abstract boolean delegable();
        
        protected abstract boolean requestGroups();

        protected boolean isTenanted() {
            return true;
        }

        protected Calendar getCalendarWithOffset(int millis) {
            return getCalendarWithOffset(Calendar.MILLISECOND, millis);
        }

        protected Calendar getCalendarWithOffset(int field, int millis) {
            Calendar cal = GregorianCalendar.getInstance();

            cal.add(field, millis);

            return cal;
        }

        public AuthToken generateCachableTokenInfo(String roles, String tokenId, String username) {
            return generateCachableTokenInfo(roles, tokenId, username, 10000);
        }

        public AuthToken generateCachableTokenInfo(String roles, String tokenId, String username, String tenantId) {
            return generateCachableTokenInfo(roles, tokenId, username, 10000, tenantId);
        }

        public AuthToken generateCachableTokenInfo(String roles, String tokenId, String username, int ttl) {
            Long expires = getCalendarWithOffset(ttl).getTimeInMillis();

            final AuthToken cti = mock(AuthToken.class);
            when(cti.getRoles()).thenReturn(roles);
            when(cti.getTokenId()).thenReturn(tokenId);
            when(cti.getUsername()).thenReturn(username);
            when(cti.getExpires()).thenReturn(expires);
            when(cti.getTenantName()).thenReturn("tenantName");
            when(cti.getTenantId()).thenReturn("tenantId");

            return cti;
        }

        public AuthToken generateCachableTokenInfo(String roles, String tokenId, String username, int ttl, String tenantId) {
            Long expires = getCalendarWithOffset(ttl).getTimeInMillis();

            final AuthToken cti = mock(AuthToken.class);
            when(cti.getRoles()).thenReturn(roles);
            when(cti.getTokenId()).thenReturn(tokenId);
            when(cti.getUsername()).thenReturn(username);
            when(cti.getExpires()).thenReturn(expires);
            when(cti.getTenantName()).thenReturn("tenantName");
            when(cti.getTenantId()).thenReturn(tenantId);

            return cti;
        }

        protected RoleList defaultRoleList() {
            //Having an empty role list is not valid, they should always have one role
            Role role = new Role();
            role.setName("derpRole");
            role.setId("derpRole");
            role.setTenantId("derpRole");
            role.setDescription("Derp description");

            RoleList roleList = new RoleList();
            roleList.getRole().add(role);
            return roleList;
        }

    }
    
    public static class TestXTenantId extends TestParent {

        private DatatypeFactory dataTypeFactory;
        AuthenticateResponse authResponse;
        Calendar expires;
        UserForAuthenticateResponse userForAuthenticateResponse;

        @Override
        protected boolean delegable() {
            return false;
        }

        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Before
        public void standUp() throws Exception {
            dataTypeFactory = DatatypeFactory.newInstance();
            expires = getCalendarWithOffset(10000000);


            when(request.getHeader(anyString())).thenReturn("tokenId");

            //building a fake authResponse
            authResponse = new AuthenticateResponse();

            //building a user to be associated with the response
            userForAuthenticateResponse = new UserForAuthenticateResponse();
            userForAuthenticateResponse.setId("104772");
            userForAuthenticateResponse.setName("user2");

        }

        @Test
        public void tenantIdFromTokenMatchesURI() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            userForAuthenticateResponse.setRoles(defaultRoleList());
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("104772");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler.handleRequest(request, response);

            Set expectedSet = new LinkedHashSet();
            expectedSet.add("104772");
            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),equalTo(expectedSet));
            assertThat(director.getFilterAction(),equalTo(FilterAction.PASS));
        }

        @Test
        public void tenantIdFromTokenMatchesAnIdFromRoles() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            Role role1 = new Role();
            role1.setName("123456");
            role1.setId("123456");
            role1.setTenantId("123456");
            role1.setDescription("Derp description");
            Role role2 = new Role();
            role2.setName("104772");
            role2.setId("104772");
            role2.setTenantId("104772");
            role2.setDescription("Derp description");
            RoleList roleList = new RoleList();
            roleList.getRole().add(role1);
            roleList.getRole().add(role2);
            userForAuthenticateResponse.setRoles(roleList);
            //build a token to go along with the auth response
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("123456");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);

            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler.handleRequest(request, response);

            Set expectedSet = new LinkedHashSet();
            expectedSet.add("104772");
            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),equalTo(expectedSet));
            assertThat(director.getFilterAction(),equalTo(FilterAction.PASS));
        }

        @Test
        public void tenantIDDoesNotMatch() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            //set the roles of the user to defaults
            userForAuthenticateResponse.setRoles(defaultRoleList());

            //build a token to go along with the auth response
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("badID");
            tenant.setName("tenantName");
            token.setTenant(tenant);

            //associate the token and user with the authresponse
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);

            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler.handleRequest(request, response);

            //this ID doesn't match so we should see FilterAction.RETURN
            assertThat(director.getFilterAction(),equalTo(FilterAction.RETURN));
        }

        @Test
        public void mossoIDTestInToken() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd/resource");
            //set the roles of the user to defaults
            userForAuthenticateResponse.setRoles(defaultRoleList());

            //build a token to go along with the auth response
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            tenant.setName("tenantName");
            token.setTenant(tenant);

            //associate the token and user with the authresponse
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);

            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler.handleRequest(request, response);

            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),notNullValue());
            Set expectedSet = new LinkedHashSet();
            expectedSet.add("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),equalTo(expectedSet));
            //we should see PASS as the filter action
            assertThat(director.getFilterAction(),equalTo(FilterAction.PASS));
        }

        @Test
        public void mossoIDTestInRoles() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd/resource");
            //set the roles of the user to defaults
            Role role1 = new Role();
            role1.setName("123456");
            role1.setId("123456");
            role1.setTenantId("123456");
            role1.setDescription("Derp description");

            Role role2 = new Role();
            role2.setName("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            role2.setId("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            role2.setTenantId("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            role2.setDescription("Derp description");

            RoleList roleList = new RoleList();
            roleList.getRole().add(role1);
            roleList.getRole().add(role2);
            userForAuthenticateResponse.setRoles(roleList);

            //build a token to go along with the auth response
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantID");
            tenant.setName("tenantName");
            token.setTenant(tenant);

            //associate the token and user with the authresponse
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);

            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler.handleRequest(request, response);

            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),notNullValue());
            Set expectedSet = new LinkedHashSet();
            expectedSet.add("MossoCloudFS_aaaa-bbbbbb-ccccc-ddddd");
            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),equalTo(expectedSet));
            //we should see PASS as the filter action
            assertThat(director.getFilterAction(),equalTo(FilterAction.PASS));
        }

    }



    public static class TestSendAllTenantIds extends TestParent {

        private DatatypeFactory dataTypeFactory;
        AuthenticateResponse authResponse;
        Calendar expires;
        UserForAuthenticateResponse userForAuthenticateResponse;
        OpenStackAuthenticationHandler handler2;

        @Override
        protected boolean delegable() {
            return false;
        }

        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Before
        public void standUp() throws Exception {
            dataTypeFactory = DatatypeFactory.newInstance();
            expires = getCalendarWithOffset(10000000);


            when(request.getHeader(anyString())).thenReturn("tokenId");

            //building a fake authResponse
            authResponse = new AuthenticateResponse();

            //building a user to be associated with the response
            userForAuthenticateResponse = new UserForAuthenticateResponse();
            userForAuthenticateResponse.setId("104772");
            userForAuthenticateResponse.setName("user2");

            final ServiceAdminRoles serviceAdminRoles = new ServiceAdminRoles();
            serviceAdminRoles.getRole().add("12345");

            Configurables configurables = new Configurables(
                    delegable(),
                    0.7,
                    "http://some.auth.endpoint",
                    keyedRegexExtractor,
                    isTenanted(),
                    AUTH_GROUP_CACHE_TTL,
                    AUTH_TOKEN_CACHE_TTL,
                    AUTH_USER_CACHE_TTL,
                    AUTH_CACHE_OFFSET,
                    requestGroups(),
                    endpointsConfiguration,
                    serviceAdminRoles.getRole(),
                    new ArrayList<String>(), true, false);
            handler2 = new OpenStackAuthenticationHandler(configurables, authService, null, null,null,null, new UriMatcher(whiteListRegexPatterns));

        }

        @Test
        public void testSendAllTenants() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            userForAuthenticateResponse.setRoles(defaultRoleList());
            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("104772");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            FilterDirector director = handler2.handleRequest(request, response);


            Set expectedSet = new LinkedHashSet();
            expectedSet.add("104772");
            expectedSet.add("derpRole");
            assertThat(director.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-tenant-id")),equalTo(expectedSet));
            assertThat(director.getFilterAction(),equalTo(FilterAction.PASS));
        }
    }





    public static class WhenCachingUserInfo extends TestParent {

        private DatatypeFactory dataTypeFactory;
        AuthenticateResponse authResponse;

        @Override
        protected boolean delegable() {
            return false;
        }
        
        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Before
        public void standUp() throws Exception {
            dataTypeFactory = DatatypeFactory.newInstance();
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            when(request.getHeader(anyString())).thenReturn("tokenId");

            Calendar expires = getCalendarWithOffset(10000000);

            authResponse = new AuthenticateResponse();
            UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
            userForAuthenticateResponse.setId("104772");
            userForAuthenticateResponse.setName("user2");

            userForAuthenticateResponse.setRoles(defaultRoleList());

            Token token = new Token();
            token.setId("tokenId");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("104772");
            tenant.setName("tenantName");
            token.setTenant(tenant);

            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);
        }

        @Test
        public void shouldCheckCacheForCredentials() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            byte[] userInfoBytes = ObjectSerializer.instance().writeObject(user);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);


            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            verify(store).get(eq(AUTH_TOKEN_CACHE_PREFIX + "." + user.getTokenId()));
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldUseCachedUserInfo() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            when(store.get(eq(AUTH_TOKEN_CACHE_PREFIX + "." + user.getTokenId()))).thenReturn(user);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            // Service should not be called if we found the token in the cache
            verify(authService, times(0)).validateToken(anyString(), anyString());
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldNotUseCachedUserInfoForExpired() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);
            when(store.get(eq(AUTH_TOKEN_CACHE_PREFIX + ".104772"))).thenReturn(user);

            // Wait until token expires
            Thread.sleep(1000);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            // Service should be called since token has expired
            verify(authService, times(1)).validateToken(anyString(), anyString());
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldNotUseCachedUserInfoForBadTokenId() throws Exception {
            authResponse.getToken().setId("differentId");
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            when(store.get(eq(AUTH_TOKEN_CACHE_PREFIX + ".104772"))).thenReturn(user);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            verify(authService, times(1)).validateToken(anyString(), anyString());
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }
    }

    public static class WhenCachingGroupInfo extends TestParent {
        private DatatypeFactory dataTypeFactory;
        AuthenticateResponse authResponse;
        Groups groups;
        Group group;

        @Override
        protected boolean delegable() {
            return false;
        }
        
        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Before
        public void standUp() throws Exception {
            dataTypeFactory = DatatypeFactory.newInstance();
            when(request.getRequestURI()).thenReturn("/start/104772/resource");
            when(request.getHeader(anyString())).thenReturn("tokenId");

            Calendar expires = getCalendarWithOffset(1000);

            groups = new Groups();
            group = new Group();
            group.setId("groupId");
            group.setDescription("Group Description");
            group.setName("Group Name");
            groups.getGroup().add(group);

            authResponse = new AuthenticateResponse();
            UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
            userForAuthenticateResponse.setId("104772");
            userForAuthenticateResponse.setName("user2");

            userForAuthenticateResponse.setRoles(defaultRoleList());

            Token token = new Token();
            token.setId("tokenId");
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("104772");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));

            authResponse.setToken(token);
            authResponse.setUser(userForAuthenticateResponse);
        }

        @Test
        public void shouldCheckCacheForGroup() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            verify(store, times(1)).get(eq(AUTH_GROUP_CACHE_PREFIX + "." + user.getTokenId()));
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldUseCachedGroupInfo() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            final AuthGroup authGroup = new OpenStackGroup(group);
            final List<AuthGroup> authGroupList = new ArrayList<AuthGroup>();
            authGroupList.add(authGroup);
            final AuthGroups groups = new AuthGroups(authGroupList);

            when(store.get(eq(AUTH_GROUP_CACHE_PREFIX + "." + user.getTokenId()))).thenReturn(groups);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            // Service should not be called if we found the token in the cache
            verify(authService, times(0)).getGroups(anyString());
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }

        @Test
        public void shouldNotUseCachedGroupInfoForExpired() throws Exception {
            final AuthToken user = new OpenStackToken(authResponse);
            when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

            final FilterDirector director = handlerWithCache.handleRequest(request, response);

            verify(store, times(1)).get(eq(AUTH_GROUP_CACHE_PREFIX + "."  + user.getTokenId()));
            // Service should be called since token has expired
            verify(authService, times(1)).getGroups(anyString());
            assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
        }
    }
    
   public static class WhenNoGroupInfo extends TestParent {
    private DatatypeFactory dataTypeFactory;
    AuthenticateResponse authResponse;
    Groups groups;
    Group group;

    @Override
    protected boolean delegable() {
        return true;
    }

    @Override
    protected boolean requestGroups() {
        return false;
    }

    @Before
    public void standUp() throws Exception {
        dataTypeFactory = DatatypeFactory.newInstance();
        when(request.getRequestURI()).thenReturn("/start/104772/resource");
        when(request.getHeader(anyString())).thenReturn("tokenId");

        Calendar expires = getCalendarWithOffset(1000);

        authResponse = new AuthenticateResponse();
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setId("104772");
        userForAuthenticateResponse.setName("user2");

        userForAuthenticateResponse.setRoles(defaultRoleList());

        Token token = new Token();
        token.setId("tokenId");
        TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
        tenant.setId("104772");
        tenant.setName("tenantName");
        token.setTenant(tenant);
        token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));

        authResponse.setToken(token);
        authResponse.setUser(userForAuthenticateResponse);

    }

   
    @Test
    public void shouldNotUseCachedGroupInfoForExpired() throws Exception {
        final AuthToken user = new OpenStackToken(authResponse);
        when(authService.validateToken(anyString(), anyString())).thenReturn(authResponse);

        final FilterDirector director = handlerWithCache.handleRequest(request, response);

        verify(store, times(1)).get(eq(AUTH_TOKEN_CACHE_PREFIX +"." + user.getTokenId()));
        // Service should be called since token has expired
        verify(authService, times(0)).getGroups(anyString());
        assertEquals("Auth component must pass valid requests", FilterAction.PASS, director.getFilterAction());
    }
    }

    public static class WhenAuthenticatingDelegatableRequests extends TestParent {

        @Override
        protected boolean delegable() {
            return true;
        }
        
        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Before
        public void standUp() {
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
        }

        @Test
        public void shouldPassNullOrBlankCredentials() {
            when(request.getRequestURI()).thenReturn("/start/");
            when(request.getHeader(anyString())).thenReturn("");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must pass requests with invalid credentials", FilterAction.PROCESS_RESPONSE, requestDirector.getFilterAction());
        }

        @Test
        public void shouldNotRejectInvalidCredentials() {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertEquals("Auth component must not reject requests with invalid credentials", FilterAction.PASS, requestDirector.getFilterAction());
        }
        
     }

    public static class WhenAuthenticatingNonDelegatableRequests extends TestParent {

        DatatypeFactory dataTypeFactory;
        Calendar expires;

        @Override
        protected boolean delegable() {
            return false;
        }
        
        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Before
        public void standUp() throws Exception {
            when(request.getRequestURI()).thenReturn("/start/12345/a/resource");
            when(request.getHeader(anyString())).thenReturn("some-random-auth-token");
            dataTypeFactory = DatatypeFactory.newInstance();
            expires = getCalendarWithOffset(10000000);
        }

        public RoleList getTwoRoles() {
            RoleList roles = new RoleList();
            Role role1 = new Role();
            role1.setName("role1");
            role1.setId("role1");
            role1.setTenantId("role2");
            role1.setDescription("role2");
            roles.getRole().add(role1);

            Role role2 = new Role();
            role2.setName("role2");
            role2.setId("role2");
            role2.setTenantId("role2");
            role2.setDescription("role2");
            roles.getRole().add(role2);
            return roles;
        }

        @Test
        public void shouldPassValidCredentials() throws Exception {
            AuthenticateResponse authResp = new AuthenticateResponse();
            UserForAuthenticateResponse user = new UserForAuthenticateResponse();
            user.setName("username");
            Token token = new Token();
            token.setTenant(new TenantForAuthenticateResponse());
            token.getTenant().setId("12345");
            token.getTenant().setName("12345");
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            authResp.setToken(token);
            authResp.setUser(user);
            authResp.getUser().setRoles(getTwoRoles());
            authResp.getToken().setId("tokentokentoken");

            when(authService.validateToken(anyString(), anyString())).thenReturn(authResp);
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
        
        @Override
        protected boolean requestGroups() {
            return false;
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

            assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap(CommonHttpHeader.WWW_AUTHENTICATE.toString())).iterator().next());
        }

        @Test
        public void shouldModifyDelegatedWwwAuthenticateHeaderOn403() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(403);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            final String expected = "Keystone uri=" + osauthConfig.getIdentityService().getUri();

            assertEquals("Auth component must pass invalid requests but process their responses", expected, responseDirector.responseHeaderManager().headersToAdd().get(HeaderName.wrap(CommonHttpHeader.WWW_AUTHENTICATE.toString())).iterator().next());
        }

        @Test
        public void shouldReturn500OnAuth501FailureWithDelegatedWwwAuthenticateHeaderSet() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Delegated");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseDirector.getResponseStatusCode());
        }
    }

    public static class WhenHandlingResponseFromServiceNotInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return false;
        }
        
        @Override
        protected boolean requestGroups() {
            return false;
        }

        @Test
        public void shouldReturn501OnAuthFailureWithNonDelegatedWwwAuthenticateHeaderSet() {
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseDirector.getResponseStatusCode());
        }

        @Test
        public void shouldReturn501OnAuthFailureWithNoWwwAuthenticateHeaderSet() {
            when(response.getStatus()).thenReturn(401);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, responseDirector.getResponseStatusCode());
        }

        @Test
        public void shouldReturn501OnAuth501FailureWithDelegatedWwwAuthenticateHeaderNotSet() {
            when(response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString())).thenReturn("Not-Delegate");
            when(response.getStatus()).thenReturn(501);

            final FilterDirector responseDirector = handler.handleResponse(request, response);

            assertEquals("Auth component must identify proxy auth failures", HttpServletResponse.SC_NOT_IMPLEMENTED, responseDirector.getResponseStatusCode());
        }
    }

    public static class WhenHandlingWhiteListNotInDelegatedMode extends TestParent {

        @Override
        protected boolean delegable() {
            return false;
        }
        
        @Override
        protected boolean requestGroups() {
            return true;
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
        
        @Override
        protected boolean requestGroups() {
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

    public static class WhenPassingRequestWithOutUserInQueryParam extends TestParent {

        @Override
        protected boolean delegable() {
            return true;
        }
        
        @Override
        protected boolean requestGroups() {
            return true;
        }

        @Test
        public void shouldNotCatchUserInQueryParam() {
            when(request.getRequestURI()).thenReturn("/v1.0/servers/service");
            when(request.getQueryString()).thenReturn("crowd=huge&username=usertest1");
            final FilterDirector requestDirector = handler.handleRequest(request, response);
            assertFalse(requestDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap("x-authorization")).toString().equalsIgnoreCase("[Proxy usertest1]"));
        }
    }
}
