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

public class ManagedRateLimitCache implements RateLimitCache {

   private final Datastore datastore;

   public ManagedRateLimitCache(Datastore datastore) {
      this.datastore = datastore;
   }

   @Override
   public Map<String, CachedRateLimit> getUserRateLimits(String account) {
      final HashMap<String, CachedRateLimit> accountRateLimitMap = getUserRateLimitMap(account);

      return Collections.unmodifiableMap(accountRateLimitMap);
   }

   private HashMap<String, CachedRateLimit> getUserRateLimitMap(String user) {
      final StoredElement element = datastore.get(user);

      return element.elementIsNull() ? new HashMap<String, CachedRateLimit>() : element.elementAs(HashMap.class);
   }

   @Override
   public NextAvailableResponse updateLimit(HttpMethod method, String user, String limitKey, ConfiguredRatelimit rateCfg) throws IOException {
      final HashMap<String, CachedRateLimit> userRateLimitMap = getUserRateLimitMap(user);

      CachedRateLimit currentLimit = userRateLimitMap != null ? userRateLimitMap.get(limitKey) : null;

      if (currentLimit == null) {
         currentLimit = new CachedRateLimit(rateCfg.getUriRegex());
         userRateLimitMap.put(limitKey, currentLimit);
      }

      final int currentLimitAmount = currentLimit.amount(method);
      final boolean hasRequests = currentLimitAmount < rateCfg.getValue();

      if (hasRequests) {
         currentLimit.logHit(method, rateCfg.getUnit());
         datastore.put(user, ObjectSerializer.instance().writeObject(userRateLimitMap), 1, TimeUnitConverter.fromSchemaTypeToConcurrent(rateCfg.getUnit()));
      }

      return new NextAvailableResponse(hasRequests, new Date(currentLimit.getEarliestExpirationTime(method)), currentLimitAmount);
   }
}
