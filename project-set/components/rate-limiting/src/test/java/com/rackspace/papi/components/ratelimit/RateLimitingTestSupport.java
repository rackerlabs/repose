package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.limits.schema.TimeUnit;
import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import com.rackspace.papi.components.ratelimit.config.RequestEndpoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class RateLimitingTestSupport {

    public static final String DEFAULT_URI = "/v1.0/*", DEFAULT_USER_ROLE = "group", DEFAULT_URI_REGEX = "/v1.0/([^/]*)/.*", DEFAULT_LIMIT_GROUP_ID = "testing-group";
    
    public static RateLimitingConfiguration defaultRateLimitingConfiguration() {
        final RateLimitingConfiguration newCfg = new RateLimitingConfiguration();

        final RequestEndpoint endpoint = new RequestEndpoint();
        endpoint.setIncludeAbsoluteLimits(Boolean.TRUE);
        endpoint.setUriRegex("/v1.0/limits/?");

        newCfg.setRequestEndpoint(endpoint);

        newCfg.getLimitGroup().add(newConfiguredLimitGroup(DEFAULT_USER_ROLE, DEFAULT_URI, DEFAULT_URI_REGEX, DEFAULT_LIMIT_GROUP_ID));

        return newCfg;
    }

    public static ConfiguredLimitGroup newConfiguredLimitGroup(String userRole, String rateLimitUri, String uriRegex, String limitGroupId) {
        final ConfiguredLimitGroup limitGroup = new ConfiguredLimitGroup();
        limitGroup.setDefault(Boolean.TRUE);
        limitGroup.setId(limitGroupId);
        limitGroup.getGroups().add(userRole);

        final ConfiguredRatelimit rateLimit = new ConfiguredRatelimit();
        rateLimit.setUnit(TimeUnit.MINUTE);
        rateLimit.getHttpMethods().add(HttpMethod.GET);
        rateLimit.setUri(rateLimitUri);
        rateLimit.setUriRegex(uriRegex);
        rateLimit.setValue(3);

        limitGroup.getLimit().add(rateLimit);

        return limitGroup;
    }

    public static Map<String, Map<String, Pattern>> newRegexCache(List<ConfiguredLimitGroup> clgList) {
        final Map<String, Map<String, Pattern>> regexCache = new HashMap<String, Map<String, Pattern>>();

        for (ConfiguredLimitGroup clg : clgList) {
            final Map<String, Pattern> limitGroupRegexCache = new HashMap<String, Pattern>();

            for (ConfiguredRatelimit crl : clg.getLimit()) {
                limitGroupRegexCache.put(crl.getUri(), Pattern.compile(crl.getUriRegex()));
            }

            regexCache.put(clg.getId(), limitGroupRegexCache);
        }
        
        return regexCache;
    }
}
