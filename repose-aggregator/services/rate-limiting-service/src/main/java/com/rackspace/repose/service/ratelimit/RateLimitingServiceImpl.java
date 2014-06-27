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
    private final RateLimiter rateLimiter;
    private final RateLimitingConfigHelper helper;
    private final boolean useCaptureGroups;

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
    public void trackLimits(String user, List<String> groups, String uri, String queryString, String httpMethod, int datastoreWarnLimit) throws OverLimitException {

        if (StringUtilities.isBlank(user)) {
            throw new IllegalArgumentException("User required when tracking rate limits.");
        }

        final ConfiguredLimitGroup configuredLimitGroup = helper.getConfiguredGroupByRole(groups);
        final List< Pair<String, ConfiguredRatelimit> > matchingConfiguredLimits = new ArrayList< Pair<String, ConfiguredRatelimit> >();
        TimeUnit largestUnit = null;

        // Go through all of the configured limits for this group
        for (ConfiguredRatelimit rateLimit : configuredLimitGroup.getLimit()) {
            Matcher uriMatcher;
            if (rateLimit instanceof ConfiguredRateLimitWrapper) {
                uriMatcher = ((ConfiguredRateLimitWrapper) rateLimit).getRegexPattern().matcher(uri);
            } else {
                LOG.error("Unable to locate pre-built regular expression pattern in for limit group.  This state is not valid. "
                        + "In order to continue operation, rate limiting will compile patterns dynamically.");
                uriMatcher = Pattern.compile(rateLimit.getUriRegex()).matcher(uri);
            }

            // Did we find a limit that matches the incoming uri, http method, and query string?
            QueryStringMatcher queryStringMatcher = newQueryStringMatcher(rateLimit.getQueryStringRegex(), queryString);
            if (uriMatcher.matches() && httpMethodMatches(rateLimit.getHttpMethods(), httpMethod) && queryStringMatcher.matches()) {
                matchingConfiguredLimits.add(Pair.of(LimitKey.getLimitKey(configuredLimitGroup.getId(),
                        rateLimit.getId(), uriMatcher, queryStringMatcher.getMatchingMatchers(), useCaptureGroups),
                        rateLimit));

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

    private QueryStringMatcher newQueryStringMatcher(String configuredQueryStringRegex, String requestQueryString) {
        /* Check pre-conditions */
        if (configuredQueryStringRegex == null || configuredQueryStringRegex.length() == 0) { return new QueryStringMatcher(true, null); }
        else if (requestQueryString == null || requestQueryString.length() == 0) { return new QueryStringMatcher(false, null); }

        ArrayList<Matcher> matchingMatchers = new ArrayList<>();

        /* The following splits should be safe since '&' is reserved as a delimiter in a query string according to
         * RFC 3986 */
        String[] configuredParameterRegexes = configuredQueryStringRegex.split("&");
        String[] requestParameters = requestQueryString.split("&");

        for (String parameterRegex : configuredParameterRegexes) {
            boolean matchFound = false;
            Pattern pattern = Pattern.compile(parameterRegex);

            for (String requestParameter : requestParameters) {
                Matcher matcher = pattern.matcher(decodeQueryString(requestParameter));
                if (matcher.matches()) {
                    matchingMatchers.add(matcher);
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) { return new QueryStringMatcher(false, null); }
        }

        return new QueryStringMatcher(true, matchingMatchers);
    }

    private String decodeQueryString(String queryString) {
        String processedQueryString = queryString;

        try {
            processedQueryString = URLDecoder.decode(processedQueryString.replace("+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            /* Since we've hardcoded the UTF-8 encoding, this should never occur. */
            LOG.error("RateLimitingService.decodeQueryString - Unsupported Encoding", uee);
        }

        return processedQueryString;
    }

    /* This class holds the result of query string matching and provides descriptive methods */
    private class QueryStringMatcher {
        private final boolean matches;
        private final List<Matcher> matchingMatchers;

        private QueryStringMatcher(boolean matches, List<Matcher> matchingMatchers) {
            this.matches = matches;
            this.matchingMatchers = matchingMatchers;
        }

        public boolean matches() {
            return matches;
        }

        public List<Matcher> getMatchingMatchers() {
            return matchingMatchers == null ? new ArrayList<Matcher>() : matchingMatchers;
        }
    }
}
