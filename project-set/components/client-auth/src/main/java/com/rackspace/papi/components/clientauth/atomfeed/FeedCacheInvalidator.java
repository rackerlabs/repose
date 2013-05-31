package com.rackspace.papi.components.clientauth.atomfeed;

import com.rackspace.papi.components.clientauth.common.AuthGroupCache;
import com.rackspace.papi.components.clientauth.common.AuthTokenCache;
import com.rackspace.papi.components.clientauth.common.AuthUserCache;
import com.rackspace.papi.components.clientauth.common.EndpointsCache;
import com.rackspace.papi.components.clientauth.openstack.v1_0.OsAuthCachePrefix;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.RsAuthCachePrefix;
import com.rackspace.papi.service.datastore.Datastore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.LoggerFactory;

/*
 * Listener class to iterate through AuthFeedReaders and retrieve items to delete from the cache
 */
public class FeedCacheInvalidator implements Runnable {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FeedCacheInvalidator.class);
   private boolean done = false;
   private List<AuthFeedReader> feeds = new ArrayList<AuthFeedReader>();
   private AuthTokenCache tknCache;
   private AuthGroupCache grpCache;
   private AuthUserCache usrCache;
   private EndpointsCache endpntsCache;
   private static final long DEFAULT_CHECK_INTERVAL = 5000;
   private long checkInterval;

   private FeedCacheInvalidator(AuthTokenCache tknCache, AuthGroupCache grpCache, AuthUserCache usrCache, EndpointsCache endpntsCache, long checkInterval) {
      this.tknCache = tknCache;
      this.grpCache = grpCache;
      this.usrCache = usrCache;
      this.endpntsCache = endpntsCache;
      this.checkInterval = checkInterval;

   }

   public static FeedCacheInvalidator openStackInstance(Datastore datastore, long checkInterval) {

      AuthTokenCache tokens = new AuthTokenCache(datastore, OsAuthCachePrefix.TOKEN.toString());
      AuthGroupCache groups = new AuthGroupCache(datastore, OsAuthCachePrefix.GROUP.toString());
      AuthUserCache users = new AuthUserCache(datastore, OsAuthCachePrefix.USER.toString());
      EndpointsCache endpntsCache = new EndpointsCache(datastore, OsAuthCachePrefix.ENDPOINTS.toString());

      return new FeedCacheInvalidator(tokens, groups, users, endpntsCache, checkInterval);

   }

   public static FeedCacheInvalidator rsAuthInstance(Datastore datastore, long checkInterval) {

      AuthTokenCache tokens = new AuthTokenCache(datastore, RsAuthCachePrefix.TOKEN.toString());
      AuthGroupCache groups = new AuthGroupCache(datastore, RsAuthCachePrefix.GROUP.toString());
      AuthUserCache users = new AuthUserCache(datastore, RsAuthCachePrefix.USER.toString());

      return new FeedCacheInvalidator(tokens, groups, users, null, checkInterval);

   }

   public static FeedCacheInvalidator rsAuthInstance(Datastore datastore) {
      return rsAuthInstance(datastore, DEFAULT_CHECK_INTERVAL);
   }

   public static FeedCacheInvalidator openStackInstance(Datastore datastore) {
      return openStackInstance(datastore, DEFAULT_CHECK_INTERVAL);
   }

   public void setFeeds(List<AuthFeedReader> feeds) {
      this.feeds = feeds;

   }

   @Override
   public void run() {
      while (!done) {

         List<String> userKeys = new ArrayList<String>();
         List<String> tokenKeys = new ArrayList<String>();

         // Iterate through atom feeds to retrieve tokens and users to invalidate from Repose cache
         for (AuthFeedReader rdr : feeds) {
            CacheKeys keys = rdr.getCacheKeys();

            userKeys.addAll(keys.getUserKeys());
            tokenKeys.addAll(keys.getTokenKeys());
         }

         tokenKeys.addAll(getTokensForUser(userKeys));
         deleteFromTokenGroupCache(tokenKeys);

         try {
            Thread.sleep(checkInterval);
         } catch (InterruptedException ex) {
            LOG.warn("Feed Cache Invalidator Thread has been interruped");
            LOG.debug("Feed Cache Invalidator Thread interruption", ex);
            Thread.currentThread().interrupt();
         }
      }
   }

   //Retrieves list of tokens associated with a user;
   private List<String> getTokensForUser(List<String> keys) {

      List<String> tokenKeys = new ArrayList<String>();
      for (String key : keys) {
         Set<String> tkns = usrCache.getUserTokenList(key);
         if (tkns != null) {
            tokenKeys.addAll(tkns);
            usrCache.deleteCacheItem(key); //removes user item from cache
            LOG.debug("Invalidating tokens from user " + key);
         }
      }

      return tokenKeys;
   }

   private void deleteFromTokenGroupCache(List<String> ids) {

      for (String id : ids) {
         LOG.debug("Invalidating token data from cache: " + id);
         deleteFromTokenGroupCache(id);
      }
   }

   private void deleteFromTokenGroupCache(String id) {
      tknCache.deleteCacheItem(id);
      grpCache.deleteCacheItem(id);
      if(endpntsCache != null){
         endpntsCache.deleteCacheItem(id);
      }
   }

   public void done() {
      done = true;
   }
}