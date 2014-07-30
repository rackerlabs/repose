package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.HttpMethod;
import com.rackspace.repose.service.limits.schema.TimeUnit;
import com.rackspace.repose.service.ratelimit.config.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RateLimitingTestSupport {

   public static final String DEFAULT_URI = "/v1.0/*", DEFAULT_USER_ROLE = "group", DEFAULT_URI_REGEX = "/v1.0/([^/]*)/.*", DEFAULT_LIMIT_GROUP_ID = "testing-group";
   public static final String MULTI_METHOD_URI = "/v2.0/*", MULTI_METHOD_URI_REGEX = "/v2.0/([^/]*)/.*", MULTI_METHOD_LIMIT_GROUP_ID = "multi-group";


   public static RateLimitingConfiguration defaultRateLimitingConfiguration() {
      final RateLimitingConfiguration newCfg = new RateLimitingConfiguration();

      final RequestEndpoint endpoint = new RequestEndpoint();
      endpoint.setIncludeAbsoluteLimits(Boolean.TRUE);
      endpoint.setUriRegex("/v1.0/limits/?");

      newCfg.setRequestEndpoint(endpoint);

      newCfg.getLimitGroup().add(newConfiguredLimitGroup(DEFAULT_USER_ROLE, DEFAULT_URI, DEFAULT_URI_REGEX, DEFAULT_LIMIT_GROUP_ID));
      newCfg.getLimitGroup().add(newMultiMethodConfiguredLimitGroup(DEFAULT_USER_ROLE, MULTI_METHOD_URI, MULTI_METHOD_URI_REGEX, MULTI_METHOD_LIMIT_GROUP_ID));

       // Global rate limiting default
       ConfiguredRatelimit globalRateLimit = new ConfiguredRatelimit();
       globalRateLimit.setId("catch-all");
       globalRateLimit.setUnit(TimeUnit.MINUTE);
       globalRateLimit.setUri(".*");
       globalRateLimit.setUriRegex(".*");
       globalRateLimit.setValue(1);
       globalRateLimit.getHttpMethods().add(HttpMethod.ALL);
       ConfiguredRateLimitWrapper globalRateLimitWrapped = new ConfiguredRateLimitWrapper(globalRateLimit);

       GlobalLimitGroup globalLimitGroup = new GlobalLimitGroup();
       globalLimitGroup.getLimit().add(globalRateLimitWrapped);

       newCfg.setGlobalLimitGroup(globalLimitGroup);
       //

      return newCfg;
   }

   public static ConfiguredLimitGroup newConfiguredLimitGroup(String userRole, String rateLimitUri, String uriRegex, String limitGroupId) {
      final int VALUE = 3;
      final ConfiguredLimitGroup limitGroup = new ConfiguredLimitGroup();
      limitGroup.setDefault(Boolean.TRUE);
      limitGroup.setId(limitGroupId);
      limitGroup.getGroups().add(userRole);

      limitGroup.getLimit().add(newConfiguredRateLimit("one", TimeUnit.MINUTE, new ArrayList<HttpMethod>(){{ add(HttpMethod.GET);}}, rateLimitUri, uriRegex, VALUE));
      limitGroup.getLimit().add(newConfiguredRateLimit("two", TimeUnit.MINUTE, new ArrayList<HttpMethod>(){{ add(HttpMethod.PUT);}}, rateLimitUri, uriRegex, VALUE));
      limitGroup.getLimit().add(newConfiguredRateLimit("three", TimeUnit.MINUTE, new ArrayList<HttpMethod>(){{ add(HttpMethod.POST);}}, rateLimitUri, uriRegex, VALUE));
      limitGroup.getLimit().add(newConfiguredRateLimit("four", TimeUnit.MINUTE, new ArrayList<HttpMethod>(){{ add(HttpMethod.DELETE);}}, rateLimitUri, uriRegex, VALUE));

      return limitGroup;
   }

   public static ConfiguredLimitGroup newMultiMethodConfiguredLimitGroup(String userRole, String rateLimitUri, String uriRegex, String limitGroupId) {
      final int VALUE = 3;
      final ConfiguredLimitGroup limitGroup = new ConfiguredLimitGroup();
      limitGroup.setDefault(Boolean.TRUE);
      limitGroup.setId(limitGroupId);
      limitGroup.getGroups().add(userRole);

      limitGroup.getLimit().add(newConfiguredRateLimit("five", TimeUnit.MINUTE,
                                                       new ArrayList<HttpMethod>(){{ add(HttpMethod.GET);
                                                                                     add(HttpMethod.PUT);
                                                                                     add(HttpMethod.POST);
                                                                                     add(HttpMethod.DELETE);}},
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
