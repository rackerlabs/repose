/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.components.clientauth.atomfeed;

import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.openstack.OpenStackToken;
import com.rackspace.papi.components.clientauth.atomfeed.sax.SaxAuthFeedReader;
import com.rackspace.papi.components.clientauth.common.AuthGroupCache;
import com.rackspace.papi.components.clientauth.common.AuthTokenCache;
import com.rackspace.papi.components.clientauth.common.AuthUserCache;
import com.rackspace.papi.components.clientauth.openstack.v1_0.OsAuthCachePrefix;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastore;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.ServiceForCatalog;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

/**
 *
 * @author malconis
 */
public class AuthFeedListenerTest {

   private Datastore datastore;
   private FeedCacheInvalidator listener;
   private AuthFeedReader rdr;
   private AuthTokenCache tkn;
   private AuthUserCache usr;
   private AuthGroupCache grp;
   private static CacheManager cacheManager;

   @Before
   public void setUp() throws IOException {

      Configuration defaultConfiguration = new Configuration();
      defaultConfiguration.setName("TestCacheManager");
      defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
      defaultConfiguration.setUpdateCheck(false);

      cacheManager = CacheManager.newInstance(defaultConfiguration);
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

      when(rdr.getCacheKeys()).thenReturn(keys);

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