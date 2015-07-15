/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.clientauth.atomfeed;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.common.auth.AuthGroups;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.common.auth.openstack.OpenStackToken;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.datastore.impl.ehcache.EHCacheDatastore;
import org.openrepose.filters.clientauth.atomfeed.sax.SaxAuthFeedReader;
import org.openrepose.filters.clientauth.common.AuthGroupCache;
import org.openrepose.filters.clientauth.common.AuthTokenCache;
import org.openrepose.filters.clientauth.common.AuthUserCache;
import org.openrepose.filters.clientauth.openstack.OsAuthCachePrefix;
import org.openstack.docs.identity.api.v2.*;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author malconis
 */
public class AuthFeedListenerTest {

    private Datastore datastore;
    private FeedCacheInvalidator listener;
    private AuthFeedReader rdr;
    private AuthTokenCache tkn;
    private AuthUserCache usr;
    private AuthGroupCache grp;

    private static AuthenticateResponse getServiceResponse() {
        AuthenticateResponse rsp = new AuthenticateResponse();

        Token token = new Token();
        token.setId("tokenid");
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, 1);
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
        user.setId("224277258");
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

    @Before
    public void setUp() throws Exception {

        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName("TestCacheManager");
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        CacheManager cacheManager = CacheManager.newInstance(defaultConfiguration);
        Cache cache = new Cache(UUID.randomUUID().toString(), 20000, false, false, 5, 2);
        cacheManager.addCache(cache);

        datastore = new EHCacheDatastore(cache);

        tkn = new AuthTokenCache(datastore, OsAuthCachePrefix.TOKEN.toString());
        grp = new AuthGroupCache(datastore, OsAuthCachePrefix.GROUP.toString());
        usr = new AuthUserCache(datastore, OsAuthCachePrefix.USER.toString());

        AuthToken token1 = new OpenStackToken(getServiceResponse());
        tkn.storeToken("token1", token1, 1000000);

        AuthGroups group = mock(AuthGroups.class);
        grp.storeGroups("group1", group, 1000000);

        Set<String> tokens = new HashSet<String>();
        tokens.add("token2");

        AuthToken token2 = new OpenStackToken(getServiceResponse());
        tkn.storeToken("token2", token2, 1000000);

        usr.storeUserTokenList("224277258", tokens, 1000000);

        listener = FeedCacheInvalidator.openStackInstance(datastore);

        rdr = mock(SaxAuthFeedReader.class);

        CacheKeys keys = new FeedCacheKeys();

        keys.addTokenKey("token1");
        keys.addUserKey("224277258");

        when(rdr.getCacheKeys(anyString())).thenReturn(keys);

    }

    @Test
    public void shouldDeleteCacheKeysFromAuthFeedResults() throws InterruptedException {

        assertNotNull("Token1 should be present in cache", tkn.getUserToken("token1"));
        assertNotNull("Token2 should be present in cache", tkn.getUserToken("token2"));
        List<AuthFeedReader> feeds = new ArrayList<AuthFeedReader>();
        feeds.add(rdr);

        listener.setFeeds(feeds);

        Thread thread = new Thread(listener);
        thread.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        listener.done();
        thread.join();

        tkn = new AuthTokenCache(datastore, OsAuthCachePrefix.TOKEN.toString());
        grp = new AuthGroupCache(datastore, OsAuthCachePrefix.GROUP.toString());
        usr = new AuthUserCache(datastore, OsAuthCachePrefix.USER.toString());


        assertNull("token1 should have been deleted from cache", tkn.getUserToken("token1"));
        assertNull("token2 should have been deleted from cache", tkn.getUserToken("token2"));

    }
}
