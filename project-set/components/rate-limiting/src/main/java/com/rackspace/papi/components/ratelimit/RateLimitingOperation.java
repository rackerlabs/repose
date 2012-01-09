package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.components.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.papi.components.ratelimit.config.RateLimitingConfiguration;
import java.util.LinkedList;
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

    protected List<ConfiguredLimitGroup> getRatelimitsForRole(String role) {
        final List<ConfiguredLimitGroup> validLimitGroupsForRole = new LinkedList<ConfiguredLimitGroup>();
        
        // Check each configured rate limit group
        for (ConfiguredLimitGroup rates : cfg.getLimitGroup()) {

            if (rates.isSetDefault() || rates.getGroups().contains(role)) {
                validLimitGroupsForRole.add(rates);
            }
        }

        // Default to empty rates if no default was set and report an error
        if (validLimitGroupsForRole.isEmpty()) {
            LOG.warn("None of the specified rate limit groups have the default parameter set. Running without a default is dangerous! Please update your config.");

            validLimitGroupsForRole.add(DEFAULT_EMPTY_LIMIT_GROUP);
        }

        // If the matched rates aren't null, return them; default otherwise.
        return validLimitGroupsForRole;
    }
}
