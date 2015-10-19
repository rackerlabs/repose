package org.openrepose.core.services.ratelimit.utils;

import org.apache.commons.lang3.tuple.Pair;
import org.openrepose.core.services.ratelimit.cache.CachedRateLimit;
import org.openrepose.core.services.ratelimit.config.ConfiguredLimitGroup;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;
import org.openrepose.core.services.ratelimit.config.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RateLimitDebugUtils {
    public static final Logger logger = LoggerFactory.getLogger(RateLimitDebugUtils.class);

    /**
     * Java sucks
     *
     * @param items
     * @param separator
     * @return
     */
    public static String joinList(List<String> items, String separator) {
        if (items.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String i : items) {
            sb.append(i).append(separator);
        }
        for (int x = 0; x < separator.length(); x++) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String debugConfiguredRatelimit(ConfiguredRatelimit crl) {

        List<String> httpMethods = new ArrayList<>(crl.getHttpMethods().size());
        for (HttpMethod method : crl.getHttpMethods()) {
            httpMethods.add(method.value());
        }

        return "ID: " + crl.getId() +
                " -- URI: " +
                crl.getUri() +
                " -- URI-REGEX: " +
                crl.getUriRegex() +
                " -- HTTP-METHODS: " +
                joinList(httpMethods, ", ") +
                " -- VALUE: " +
                crl.getValue() +
                " -- UNIT: " +
                crl.getUnit() +
                " -- QUERY-PARAM-NAMES: " +
                joinList(crl.getQueryParamNames(), ", ");

    }

    public static String debugCachedRateLimit(CachedRateLimit crl) {
        if (crl == null) {
            return "NULL CACHED RATE LIMIT";
        }
        return "CACHED LIMIT: " +
                " CONFIGID: " + crl.getConfigId() +
                " -- MAXCOUNT: " + crl.maxAmount() +
                " -- AMOUNT: " + crl.amount() +
                " -- UNIT: " + crl.unit(); //WHY THE HELL IS THIS IN MILLIS?

    }

    public static void debugLogMatchingLimitsList(String user, List<Pair<String, ConfiguredRatelimit>> limits) {
        logger.debug("LIMITS LIST FOR {}", user);

        for (Pair<String, ConfiguredRatelimit> pair : limits) {
            logger.debug("CONFIGURED RATE LIMIT: {}", pair.getLeft());

            ConfiguredRatelimit r = pair.getRight();
            logger.debug(debugConfiguredRatelimit(r));
        }
    }

    public static void debugLogCachedLimits(Map<String, CachedRateLimit> cachedLimits) {
        for (String key : cachedLimits.keySet()) {
            CachedRateLimit limit = cachedLimits.get(key);
            logger.debug("CACHED LIMIT FOR {}: {}",
                    key,
                    debugCachedRateLimit(limit));
        }
    }

    public static void debugLogConfiguredLimitGroup(ConfiguredLimitGroup limitGroup) {
        StringBuilder sb = new StringBuilder();
        for (String group : limitGroup.getGroups()) {
            sb.append(group).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        logger.debug("ID: {} CONFIGURED LIMIT GROUP GROUPS: {}", limitGroup.getId(), sb.toString());

        for (ConfiguredRatelimit limit : limitGroup.getLimit()) {
            logger.debug("ID: {} -- {}", limitGroup.getId(), debugConfiguredRatelimit(limit));
        }
    }
}
