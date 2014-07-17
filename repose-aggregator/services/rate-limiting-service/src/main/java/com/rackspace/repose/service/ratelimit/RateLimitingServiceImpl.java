package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.cache.CachedRateLimit;
import com.rackspace.repose.service.ratelimit.cache.RateLimitCache;
import com.rackspace.repose.service.ratelimit.config.*;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;
import com.rackspace.repose.service.ratelimit.util.StringUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingServiceImpl.class);
    private final RateLimitCache cache;
    private final RateLimitingConfigHelper helper;
    private final boolean useCaptureGroups;

    private RateLimiter rateLimiter;

    public RateLimitingServiceImpl(RateLimitCache cache, RateLimitingConfiguration rateLimitingConfiguration) {

        if (rateLimitingConfiguration == null) {
            throw new IllegalArgumentException("Rate limiting configuration must not be null.");
        }

        this.cache = cache;
        this.rateLimiter = new RateLimiter(cache);
        this.helper = new RateLimitingConfigHelper(rateLimitingConfiguration);
        useCaptureGroups = rateLimitingConfiguration.isUseCaptureGroups();
    }

    @Override
    public RateLimitList queryLimits(String user, List<String> groups) {

        if (StringUtilities.isBlank(user)) {
            throw new IllegalArgumentException("User required when querying rate limits.");
        }

        final Map<String, CachedRateLimit> cachedLimits = cache.getUserRateLimits(user);
        final ConfiguredLimitGroup configuredLimitGroup = helper.getConfiguredGroupByRole(groups);
        final RateLimitListBuilder limitsBuilder = new RateLimitListBuilder(cachedLimits, configuredLimitGroup);

        return limitsBuilder.toRateLimitList();
    }

    @Override
    public void trackLimits(String user, List<String> groups, String uri, Map<String, String[]> parameterMap, String httpMethod, int datastoreWarnLimit) throws OverLimitException {

        if (StringUtilities.isBlank(user)) {
            throw new IllegalArgumentException("User required when tracking rate limits.");
        }

        final ConfiguredLimitGroup configuredLimitGroup = helper.getConfiguredGroupByRole(groups);
        final List< Pair<String, ConfiguredRatelimit> > matchingConfiguredLimits = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        TimeUnit largestUnit = null;

        // Go through all of the configured limits for this group
        // TODO: This collection should /always/ include the global limit group
        for (ConfiguredRatelimit rateLimit : configuredLimitGroup.getLimit()) {
            Matcher uriMatcher;
            if (rateLimit instanceof ConfiguredRateLimitWrapper) {
                uriMatcher = ((ConfiguredRateLimitWrapper) rateLimit).getRegexPattern().matcher(uri);
            } else {
                LOG.error("Unable to locate pre-built regular expression pattern in for limit group.  This state is not valid. "
                        + "In order to continue operation, rate limiting will compile patterns dynamically.");
                uriMatcher = Pattern.compile(rateLimit.getUriRegex()).matcher(uri);
            }

            // Did we find a limit that matches the incoming uri and http method?
            // TODO: This conditional should always match if a rate limit is a global rate limit
            if (uriMatcher.matches() && httpMethodMatches(rateLimit.getHttpMethods(), httpMethod) && queryParameterNameMatches(rateLimit.getQueryParamNames(), parameterMap)) {
                matchingConfiguredLimits.add(Pair.of(LimitKey.getLimitKey(configuredLimitGroup.getId(),
                        rateLimit.getId(), uriMatcher, useCaptureGroups), rateLimit));

                if (largestUnit == null || rateLimit.getUnit().compareTo(largestUnit) > 0) {
                    largestUnit = rateLimit.getUnit();
                }
            }
        }
        if (matchingConfiguredLimits.size() > 0) {
            rateLimiter.handleRateLimit(user, matchingConfiguredLimits, largestUnit, datastoreWarnLimit);
        }
    }

    private boolean httpMethodMatches(List<HttpMethod> configMethods, String requestMethod) {
        return configMethods.contains(HttpMethod.ALL) || configMethods.contains(HttpMethod.valueOf(requestMethod.toUpperCase()));
    }

    private boolean queryParameterNameMatches(List<String> configuredQueryParams, Map<String, String[]> requestParameterMap) {
        for (String configuredParamKey : configuredQueryParams) {
            boolean matchFound = false;

            for (String requestParamKey : requestParameterMap.keySet()) {
                if (decodeQueryString(configuredParamKey).equalsIgnoreCase(decodeQueryString(requestParamKey))) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) { return false; }
        }
        return true;
    }

    private String decodeQueryString(String queryString) {
        String processedQueryString = queryString;

        try {
            processedQueryString = URLDecoder.decode(processedQueryString, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            /* Since we've hardcoded the UTF-8 encoding, this should never occur. */
            LOG.error("RateLimitingService.decodeQueryString - Unsupported Encoding", uee);
        }

        return processedQueryString;
    }
}
