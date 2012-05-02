package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.LimitsFormat;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RateLimitingOperation {

   private static final Logger LOG = LoggerFactory.getLogger(RateLimitingOperation.class);
   protected static final ConfiguredLimitGroup DEFAULT_EMPTY_LIMIT_GROUP = new ConfiguredLimitGroup();
   
   private final RateLimitingConfiguration cfg;

   public RateLimitingOperation(RateLimitingConfiguration cfg) {
      this.cfg = cfg;
   }

   public LimitsFormat getLimitsFormat() {
      return this.cfg.getRequestEndpoint().getLimitsFormat();
   }

   protected ConfiguredLimitGroup getRateLimitGroupForRole(List<? extends HeaderValue> roles) {
      ConfiguredLimitGroup defaultLimitGroup = null;

      // Check each configured rate limit group
      for (ConfiguredLimitGroup configuredRateLimits : cfg.getLimitGroup()) {
         if (configuredRateLimits.isDefault()) {
            defaultLimitGroup = configuredRateLimits;
         }

         for (HeaderValue role : roles) {
            if (configuredRateLimits.getGroups().contains(role.getValue())) {
               return configuredRateLimits;
            }
         }
      }

      // Default to empty rates if no default was set and report an error
      if (defaultLimitGroup == null) {
         LOG.warn("None of the specified rate limit groups have the default parameter set. Running without a default is dangerous! Please update your config.");
         defaultLimitGroup = DEFAULT_EMPTY_LIMIT_GROUP;
      }

      // If the matched rates aren't null, return them; default otherwise.
      return defaultLimitGroup;
   }
}
