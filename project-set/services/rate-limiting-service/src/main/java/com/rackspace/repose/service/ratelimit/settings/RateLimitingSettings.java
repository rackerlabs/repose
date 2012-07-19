package com.rackspace.repose.service.ratelimit.settings;

import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.repose.service.ratelimit.util.RateLimitKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RateLimitingSettings {

   private static final Logger LOG = LoggerFactory.getLogger(RateLimitingSettings.class);
   private final Map<String, Map<String, Pattern>> regexCache;
   private final List<ConfiguredLimitGroup> configuredLimitGroups;

   private RateLimitingSettings(Map<String, Map<String, Pattern>> regexCache, List<ConfiguredLimitGroup> configuredLimitGroups) {
      this.regexCache = regexCache;
      this.configuredLimitGroups = configuredLimitGroups;
   }

   public static RateLimitingSettings build(Map<String, Map<String, Pattern>> regexCache, List<ConfiguredLimitGroup> configuredLimitGroups) {
      return new RateLimitingSettings(Collections.unmodifiableMap(regexCache), configuredLimitGroups);
   }

   public Pattern getPatternFromCache(String configuredLimitGroupId, ConfiguredRatelimit rateLimit) {
      final Map<String, Pattern> ratesRegexCache = regexCache.get(configuredLimitGroupId);
      Pattern uriRegexPattern = ratesRegexCache != null ? ratesRegexCache.get(RateLimitKeyGenerator.createMapKey(rateLimit)) : null;

      if (uriRegexPattern == null) {
         LOG.error("Unable to locate prebuilt regular expression pattern in "
                 + "rate limiting's regex cache - this state is not valid. "
                 + "In order to continue operation, rate limiting will compile patterns dynamically.");

         uriRegexPattern = Pattern.compile(rateLimit.getUriRegex());
      }

      return uriRegexPattern;
   }

   public ConfiguredLimitGroup getConfiguredGroupByRole(List<String> roles) {
      final ConfiguredLimitGroup DEFAULT_EMPTY_LIMIT_GROUP = new ConfiguredLimitGroup();
      ConfiguredLimitGroup defaultLimitGroup = null;

      // Check each configured rate limit group
      for (ConfiguredLimitGroup configuredRateLimits : configuredLimitGroups) {
         if (configuredRateLimits.isDefault()) {
            defaultLimitGroup = configuredRateLimits;
         }

         for (String role : roles) {
            if (configuredRateLimits.getGroups().contains(role)) {
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
