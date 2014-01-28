package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.papi.components.datastore.Datastore;
import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
      final Serializable element = datastore.get(user);

      return (element == null) ? new HashMap<String, CachedRateLimit>() : ((UserRateLimit) element).getLimitMap();
   }

   @Override
   public NextAvailableResponse updateLimit(HttpMethod method, String user, String limitKey, ConfiguredRatelimit rateCfg, int datastoreWarnLimit) throws IOException {
        UserRateLimitResult patchResult = (UserRateLimitResult)datastore.patch(user, new UserRateLimit.Patch(), 1, TimeUnitConverter.fromSchemaTypeToConcurrent(rateCfg.getUnit()));

        if(patchResult.getLimitMap().keySet().size() >= datastoreWarnLimit){
            LOG.warn("Large amount of limits recorded.  Repose Rate Limited may be misconfigured, keeping track of rate limits for user: "+ user +". Please review capture groups in your rate limit configuration.  If using clustered datastore, you may experience network latency.");
        }

        CachedRateLimit rateLimit = patchResult.getLimitMap().get(limitKey);
        return new NextAvailableResponse(patchResult.getSuccess(), new Date(rateLimit.getEarliestExpirationTime(method)), rateLimit.amount(method));
   }
}
