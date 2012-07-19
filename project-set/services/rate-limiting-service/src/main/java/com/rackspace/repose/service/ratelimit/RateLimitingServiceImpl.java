package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.util.StringUtilities;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.exception.*;
import com.rackspace.repose.service.ratelimit.settings.RateLimitingSettings;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class RateLimitingServiceImpl implements RateLimitingService {

   private final RateLimitCache cache;
   private final RateLimitingSettings settings;
   private final RateLimiter rateLimiter;

   public RateLimitingServiceImpl(RateLimitCache cache, RateLimitingSettings settings) {

      if (settings == null) {
         throw new RateLimitingConfigurationException("Rate limiting configuration must not be null.");
      }

      this.cache = cache;
      this.settings = settings;
      this.rateLimiter = new RateLimiter(cache);
   }

   @Override
   public RateLimitList queryLimits(String user, List<String> groups) {

      if (StringUtilities.isBlank(user)) {
         throw new UnknownUserException("User required when querying rate limits.");
      }

      final Map<String, CachedRateLimit> cachedLimits = cache.getUserRateLimits(user);
      final ConfiguredLimitGroup configuredLimitGroup = settings.getConfiguredGroupByRole(groups);
      final RateLimitListBuilder limitsBuilder = new RateLimitListBuilder(cachedLimits, configuredLimitGroup);

      return limitsBuilder.toRateLimitList();
   }

   @Override
   public void trackLimits(String user, List<String> groups, String uri, String httpMethod) throws OverLimitException {

      if (StringUtilities.isBlank(user)) {
         throw new UnknownUserException("User required when tracking rate limits.");
      }

      final ConfiguredLimitGroup configuredLimitGroup = settings.getConfiguredGroupByRole(groups);

      // Go through all of the configured limits for this group
      for (ConfiguredRatelimit rateLimit : configuredLimitGroup.getLimit()) {
         final Matcher uriMatcher = settings.getPatternFromCache(configuredLimitGroup.getId(), rateLimit).matcher(uri);

         // Did we find a limit that matches the incoming uri and http method?
         if (uriMatcher.matches() && httpMethodMatches(rateLimit.getHttpMethods(), httpMethod)) {
            rateLimiter.handleRateLimit(user, new LimitKey().getLimitKey(uri, uriMatcher), rateLimit);
            return;
         }
      }
   }

   private boolean httpMethodMatches(List<HttpMethod> configMethods, String requestMethod) {
      return (configMethods.contains(HttpMethod.ALL) || configMethods.contains(HttpMethod.valueOf(requestMethod.toUpperCase())));
   }
}
