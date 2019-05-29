/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.ratelimiting;

import org.openrepose.core.services.datastore.Datastore;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.ratelimit.RateLimitingService;
import org.openrepose.core.services.ratelimit.RateLimitingServiceFactory;
import org.openrepose.core.services.ratelimit.cache.ManagedRateLimitCache;
import org.openrepose.core.services.ratelimit.cache.RateLimitCache;
import org.openrepose.core.services.ratelimit.config.*;
import org.openrepose.filters.ratelimiting.write.ActiveLimitsWriter;
import org.openrepose.filters.ratelimiting.write.CombinedLimitsWriter;

import java.util.*;
import java.util.regex.Pattern;

public abstract class RateLimitingTestSupport {

    public static final String DEFAULT_URI = "/v1.0/*", DEFAULT_USER_ROLE = "group", DEFAULT_URI_REGEX = "/v1.0/([^/]*)/.*", DEFAULT_LIMIT_GROUP_ID = "testing-group";
    public static final String MULTI_METHOD_URI = "/v2.0/*", MULTI_METHOD_URI_REGEX = "/v2.0/([^/]*)/.*", MULTI_METHOD_LIMIT_GROUP_ID = "multi-group";

    public static RateLimitingHandler createHandler(RateLimitingConfiguration configurationObject, EventService eventService, Datastore datastore) {
        RateLimitCache rateLimitCache = new ManagedRateLimitCache(datastore);
        RateLimitingService rateLimitingService = RateLimitingServiceFactory.createRateLimitingService(rateLimitCache, configurationObject);
        Optional<Pattern> describeLimitsUriRegex = configurationObject.getRequestEndpoint() != null ?
                Optional.of(Pattern.compile(configurationObject.getRequestEndpoint().getUriRegex())) :
                Optional.empty();
        boolean includeAbsoluteLimits = configurationObject.getRequestEndpoint() != null &&
                configurationObject.getRequestEndpoint().isIncludeAbsoluteLimits();

        return new RateLimitingHandler(
                new RateLimitingServiceHelper(rateLimitingService, new ActiveLimitsWriter(), new CombinedLimitsWriter()),
                eventService,
                includeAbsoluteLimits,
                describeLimitsUriRegex,
                configurationObject.isOverLimit429ResponseCode(),
                configurationObject.getDatastoreWarnLimit().intValue());
    }


    public static RateLimitingConfiguration defaultRateLimitingConfiguration() {
        final RateLimitingConfiguration newCfg = new RateLimitingConfiguration();

        final RequestEndpoint endpoint = new RequestEndpoint();
        endpoint.setIncludeAbsoluteLimits(Boolean.TRUE);
        endpoint.setUriRegex("/v1.0/limits/?");

        newCfg.setRequestEndpoint(endpoint);

        newCfg.getLimitGroup().add(newConfiguredLimitGroup(DEFAULT_USER_ROLE, DEFAULT_URI, DEFAULT_URI_REGEX, DEFAULT_LIMIT_GROUP_ID));
        newCfg.getLimitGroup().add(newMultiMethodConfiguredLimitGroup(DEFAULT_USER_ROLE, MULTI_METHOD_URI, MULTI_METHOD_URI_REGEX, MULTI_METHOD_LIMIT_GROUP_ID));

        return newCfg;
    }

    public static ConfiguredLimitGroup newConfiguredLimitGroup(String userRole, String rateLimitUri, String uriRegex, String limitGroupId) {
        final int VALUE = 3;
        final ConfiguredLimitGroup limitGroup = new ConfiguredLimitGroup();
        limitGroup.setDefault(Boolean.TRUE);
        limitGroup.setId(limitGroupId);
        limitGroup.getGroups().add(userRole);

        limitGroup.getLimit().add(newConfiguredRateLimit("one", TimeUnit.MINUTE, new ArrayList<HttpMethod>() {{
            add(HttpMethod.GET);
        }}, rateLimitUri, uriRegex, VALUE));
        limitGroup.getLimit().add(newConfiguredRateLimit("two", TimeUnit.MINUTE, new ArrayList<HttpMethod>() {{
            add(HttpMethod.PUT);
        }}, rateLimitUri, uriRegex, VALUE));
        limitGroup.getLimit().add(newConfiguredRateLimit("three", TimeUnit.MINUTE, new ArrayList<HttpMethod>() {{
            add(HttpMethod.POST);
        }}, rateLimitUri, uriRegex, VALUE));
        limitGroup.getLimit().add(newConfiguredRateLimit("four", TimeUnit.MINUTE, new ArrayList<HttpMethod>() {{
            add(HttpMethod.DELETE);
        }}, rateLimitUri, uriRegex, VALUE));

        return limitGroup;
    }

    public static ConfiguredLimitGroup newMultiMethodConfiguredLimitGroup(String userRole, String rateLimitUri, String uriRegex, String limitGroupId) {
        final int VALUE = 3;
        final ConfiguredLimitGroup limitGroup = new ConfiguredLimitGroup();
        limitGroup.setDefault(Boolean.FALSE);
        limitGroup.setId(limitGroupId);
        limitGroup.getGroups().add(userRole);

        limitGroup.getLimit().add(newConfiguredRateLimit("one", TimeUnit.MINUTE,
                new ArrayList<HttpMethod>() {{
                    add(HttpMethod.GET);
                    add(HttpMethod.PUT);
                    add(HttpMethod.POST);
                    add(HttpMethod.DELETE);
                }},
                rateLimitUri, uriRegex, VALUE));

        return limitGroup;
    }

    public static ConfiguredRatelimit newConfiguredRateLimit(String id, TimeUnit unit, List<HttpMethod> methods, String rateLimitUri, String uriRegex, int value) {
        final ConfiguredRatelimit rateLimit = new ConfiguredRatelimit();

        rateLimit.setId(id);
        rateLimit.setUnit(unit);
        rateLimit.setUri(rateLimitUri);
        rateLimit.setUriRegex(uriRegex);
        rateLimit.setValue(value);

        for (HttpMethod method : methods) {
            rateLimit.getHttpMethods().add(method);
        }

        return rateLimit;
    }

    public static Map<String, Map<String, Pattern>> newRegexCache(List<ConfiguredLimitGroup> clgList) {
        final Map<String, Map<String, Pattern>> regexCache = new HashMap<>();

        for (ConfiguredLimitGroup clg : clgList) {
            final Map<String, Pattern> limitGroupRegexCache = new HashMap<>();

            for (ConfiguredRatelimit crl : clg.getLimit()) {
                limitGroupRegexCache.put(crl.getUri(), Pattern.compile(crl.getUriRegex()));
            }

            regexCache.put(clg.getId(), limitGroupRegexCache);
        }

        return regexCache;
    }
}
