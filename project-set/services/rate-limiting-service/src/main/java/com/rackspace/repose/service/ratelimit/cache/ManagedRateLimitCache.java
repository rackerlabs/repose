package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.ratelimit.cache.util.ObjectSerializer;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;

import com.rackspace.repose.service.limits.schema.HttpMethod;

// TODO: still dependency on repose core here 
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.StoredElement;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;


/* Responsible for updating and querying ratelimits in cache */
public class ManagedRateLimitCache implements RateLimitCache {
    
  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ManagedRateLimitCache.class);

   private final Datastore datastore;

   public ManagedRateLimitCache(Datastore datastore) {
      this.datastore = datastore;
   }

   @Override
   public Map<String, CachedRateLimit> getUserRateLimits(String account) {
      final Map<String, CachedRateLimit> accountRateLimitMap = getUserRateLimitMap(account);

      return Collections.unmodifiableMap(accountRateLimitMap);
   }

   private Map<String, CachedRateLimit> getUserRateLimitMap(String user) {
      final StoredElement element = datastore.get(user);

      return element.elementIsNull() ? new HashMap<String, CachedRateLimit>() : element.elementAs(HashMap.class);
   }

   @Override
   public NextAvailableResponse updateLimit(HttpMethod method, String user, String limitKey, ConfiguredRatelimit rateCfg, int datastoreWarnLimit) throws IOException {
      final Map<String, CachedRateLimit> userRateLimitMap = getUserRateLimitMap(user);
 
      if(userRateLimitMap.keySet().size() >= datastoreWarnLimit){
          LOG.warn("Large amount of limits recorded.  Repose Rate Limited may be misconfigured, keeping track of rate limits for user: "+ user +". Please review capture groups in your rate limit configuration.  If using clustered datastore, you may experience network latency.");
      }
      
      CachedRateLimit currentLimit = userRateLimitMap != null ? userRateLimitMap.get(limitKey) : null;

      if (currentLimit == null) {
         currentLimit = new CachedRateLimit(rateCfg.getUriRegex());
         userRateLimitMap.put(limitKey, currentLimit);
      }

      final int currentLimitAmount = currentLimit.amount(method);
      final boolean hasRequests = currentLimitAmount < rateCfg.getValue();

      if (hasRequests) {
         currentLimit.logHit(method, rateCfg.getUnit());
         datastore.put(user, ObjectSerializer.instance().writeObject((HashMap<String, CachedRateLimit>)userRateLimitMap), 1, TimeUnitConverter.fromSchemaTypeToConcurrent(rateCfg.getUnit()));
      }

      return new NextAvailableResponse(hasRequests, new Date(currentLimit.getEarliestExpirationTime(method)), currentLimitAmount);
   }
}
