package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpDate;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.ratelimit.cache.NextAvailableResponse;
import com.rackspace.papi.components.ratelimit.cache.RateLimitCache;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.components.ratelimit.util.RateLimitKeyGenerator;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jhopper
 */
public class RateLimiter extends RateLimitingOperation {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimiter.class);
   private final RateLimitCache cache;
   private final Map<String, Map<String, Pattern>> regexCache;
   private final boolean responseDelegationEnabled;

   public RateLimiter(RateLimitCache cache, Map<String, Map<String, Pattern>> regexCache, RateLimitingConfiguration cfg) {
      super(cfg);

      this.cache = cache;
      this.regexCache = copyRegexMap(regexCache);
      this.responseDelegationEnabled = cfg.isDelegation();
   }

   private Map<String, Map<String, Pattern>> copyRegexMap(Map<String, Map<String, Pattern>> regexCache) {
      final Map<String, Map<String, Pattern>> deepRegexMapCopy = new HashMap<String, Map<String, Pattern>>();

      for (Map.Entry<String, Map<String, Pattern>> cachedRegexMapEntry : regexCache.entrySet()) {
         final Map<String, Pattern> copy = new HashMap<String, Pattern>(cachedRegexMapEntry.getValue());

         deepRegexMapCopy.put(cachedRegexMapEntry.getKey(), copy);
      }

      return deepRegexMapCopy;
   }

   public void recordLimitedRequest(RateLimitingRequestInfo requestInfo, FilterDirector filterDirector) {
      final String requestUri = requestInfo.getRequest().getRequestURI();
      final ConfiguredLimitGroup currentLimitGroup = getRateLimitGroupForRole(requestInfo.getUserGroups());

      // Go through all of the configured limits for this group
      for (ConfiguredRatelimit rateLimit : currentLimitGroup.getLimit()) {
         final Pattern p = getPattern(currentLimitGroup.getId(), rateLimit);
         final Matcher uriMatcher = p.matcher(requestUri);

         // Did we find a limit that matches the current request?
         if (uriMatcher.matches() && httpMethodMatches(rateLimit.getHttpMethods(), requestInfo.getRequestMethod())) {
            handleRateLimit(requestInfo, uriMatcher, rateLimit, filterDirector);
            return;
         }
      }
   }

   private boolean httpMethodMatches(List<HttpMethod> configMethods, HttpMethod requestMethod) {
      return (configMethods.contains(HttpMethod.ALL) || configMethods.contains(requestMethod));
   }

   private Pattern getPattern(String appliedRatesId, ConfiguredRatelimit rateLimit) {
      final Map<String, Pattern> ratesRegexCache = regexCache.get(appliedRatesId);
      Pattern uriRegexPattern = ratesRegexCache != null ? ratesRegexCache.get(RateLimitKeyGenerator.createMapKey(rateLimit)) : null;

      if (uriRegexPattern == null) {
         LOG.error("Unable to locate prebuilt regular expression pattern in "
                 + "rate limiting's regex cache - this state is not valid. "
                 + "In order to continue operation, rate limiting will compile patterns dynamically.");

         uriRegexPattern = Pattern.compile(rateLimit.getUriRegex());
      }

      return uriRegexPattern;
   }

   private void handleRateLimit(RateLimitingRequestInfo requestInfo, Matcher uriMatcher, ConfiguredRatelimit rateLimit, FilterDirector filterDirector) {
      // The group count represents the number of elments that will go into 
      // generating the unique cache id for the requested URI
      final int groupCount = uriMatcher.groupCount();

      final StringBuilder cacheIdBuffer = new StringBuilder();

      // Do we have any groups to use for generating our cache ID?
      if (groupCount > 0) {

         // Since these are regex groups we start at 1 since regex group 0 always
         // stands for the entire expression
         for (int i = 1; i <= groupCount; i++) {
            cacheIdBuffer.append(uriMatcher.group(i));
         }
      } else {
         // We default to the whole URI in the case where no regex group info was provided
         LOG.warn("Using regex caputure groups is recommended to help Repose build replicable, meaningful cache IDs for rate limits. Please update your config.");
         cacheIdBuffer.append(requestInfo.getRequest().getRequestURI());
      }

      // Get the next, shortest available time that a user has to wait for
      try {
         for (HttpMethod configuredMethod : rateLimit.getHttpMethods()) {
            final NextAvailableResponse nextAvailable = cache.updateLimit(configuredMethod, requestInfo.getUserName().getValue(), cacheIdBuffer.toString(), rateLimit);

            if (!nextAvailable.hasRequestsRemaining()) {
               prepareNextAvailableResponse(nextAvailable, filterDirector);

               final UserIdentificationSanitizer sanitizer = new UserIdentificationSanitizer(requestInfo);
               LOG.info("Rate limiting user " + sanitizer.getUserIdentification() + " at limit amount " + nextAvailable.getCurrentLimitAmount() + ".");
               LOG.info("User rate limited for request " + requestInfo.getRequestMethod().value() + " " + requestInfo.getRequest().getRequestURL() + ".");
               LOG.info("Configured rate limit is: " + rateLimit.toString());
            }
         }
      } catch (IOException ioe) {
         LOG.error("IOException caught during cache commit for rate limit user: " + requestInfo.getUserName()
                 + " - Reason: " + ioe.getMessage(), ioe);

         filterDirector.setFilterAction(FilterAction.RETURN);
         filterDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
      }
   }

   private void prepareNextAvailableResponse(NextAvailableResponse nextAvailable, FilterDirector filterDirector) {
      final HttpDate nextAvailableTime = new HttpDate(nextAvailable.getResetTime());

      if (!responseDelegationEnabled) {
         // Tell the filter we want to return right away
         filterDirector.setFilterAction(FilterAction.RETURN);

         // We use a 413 "Request Entity Too Large" to communicate that the user
         // in question has hit their rate limit for this requested URI
         filterDirector.setResponseStatus(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE);
         filterDirector.responseHeaderManager().appendHeader(CommonHttpHeader.RETRY_AFTER.toString(), nextAvailableTime.toRFC1123());
      } else {
         // When response delegation is enabled, we add the X-PP-Rate-Limited
         // request header with its value set to the correctly formatted 
         // retry-after date that would have been returned to the client
         filterDirector.setFilterAction(FilterAction.PASS);
         filterDirector.requestHeaderManager().putHeader(PowerApiHeader.RATE_LIMITED.toString(), nextAvailableTime.toRFC1123());
      }
   }
}
