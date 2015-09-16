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

import org.openrepose.commons.utils.logging.TracingKey;
import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.filters.clientauth.common.AuthGroupCache;
import org.openrepose.filters.clientauth.common.AuthTokenCache;
import org.openrepose.filters.clientauth.common.AuthUserCache;
import org.openrepose.filters.clientauth.common.EndpointsCache;
import org.openrepose.filters.clientauth.openstack.OsAuthCachePrefix;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 * Listener class to iterate through AuthFeedReaders and retrieve items to delete from the cache
 */
@Deprecated
public class FeedCacheInvalidator implements Runnable {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FeedCacheInvalidator.class);
    private static final long DEFAULT_CHECK_INTERVAL = 5000;
    private boolean done = false;
    private List<AuthFeedReader> feeds = new ArrayList<>();
    private AuthTokenCache tknCache;
    private AuthGroupCache grpCache;
    private AuthUserCache usrCache;
    private EndpointsCache endpntsCache;
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

    public static FeedCacheInvalidator openStackInstance(Datastore datastore) {
        return openStackInstance(datastore, DEFAULT_CHECK_INTERVAL);
    }

    public void setFeeds(List<AuthFeedReader> feeds) {
        this.feeds = feeds;
    }

    public void setOutboundTracing(boolean isOutboundTracing) {
        for (AuthFeedReader afr : feeds) {
            afr.setOutboundTracing(isOutboundTracing);
        }
    }

    @Override
    public void run() {

        while (!done) {
            // Generate trans-id here so it is the same between multiple pages
            String traceID = UUID.randomUUID().toString();

            MDC.put(TracingKey.TRACING_KEY, traceID);

            LOG.debug("Beginning Feed Cache Invalidator Thread request.");

            List<String> userKeys = new ArrayList<>();
            List<String> tokenKeys = new ArrayList<>();

            // Iterate through atom feeds to retrieve tokens and users to invalidate from Repose cache
            for (AuthFeedReader rdr : feeds) {
                try {
                    CacheKeys keys = rdr.getCacheKeys(traceID);
                    userKeys.addAll(keys.getUserKeys());
                    tokenKeys.addAll(keys.getTokenKeys());
                } catch (FeedException e) {
                    LOG.error("Unable get Cached Keys.");
                    LOG.trace("", e);
                }
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

    /**
     * Retrieves list of tokens associated with a user.
     */
    private List<String> getTokensForUser(List<String> keys) {

        List<String> tokenKeys = new ArrayList<>();
        for (String key : keys) {
            Set<String> tkns = usrCache.getUserTokenList(key);
            if (tkns != null) {
                tokenKeys.addAll(tkns);
                //removes user item from cache
                usrCache.deleteCacheItem(key);
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
        if (endpntsCache != null) {
            endpntsCache.deleteCacheItem(id);
        }
    }

    public void done() {
        done = true;
    }
}
