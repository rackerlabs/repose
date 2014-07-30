package com.rackspace.repose.service.ratelimit.config;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RateLimitingConfigHelper {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RateLimitingConfigHelper.class);

    private final GlobalLimitGroup globalLimitGroup;
    private final List<ConfiguredLimitGroup> configuredLimitGroups;

    public RateLimitingConfigHelper(RateLimitingConfiguration rateLimitingConfiguration) {
        this.globalLimitGroup = processGlobalLimits(rateLimitingConfiguration.getGlobalLimitGroup());
        this.configuredLimitGroups = processConfiguration(rateLimitingConfiguration);
    }

    /**
     * @param groups The user's groups
     * @return Returns empty ConfiguredLimitGroup by default if groups do not match any configured limit groups.
     */
    public ConfiguredLimitGroup getConfiguredGroupByRole(List<String> groups) {
        ConfiguredLimitGroup defaultLimitGroup = new ConfiguredLimitGroup();

        // Check each configured rate limit group
        for (ConfiguredLimitGroup configuredLimitGroup : configuredLimitGroups) {
            if (configuredLimitGroup.isDefault()) {
                defaultLimitGroup = configuredLimitGroup;
            }

            for (String group : groups) {
                if (configuredLimitGroup.getGroups().contains(group)) {
                    return configuredLimitGroup;
                }
            }
        }

        return defaultLimitGroup;
    }

    public GlobalLimitGroup getGlobalLimitGroup() {
        return globalLimitGroup;
    }

    private List<ConfiguredLimitGroup> processConfiguration(RateLimitingConfiguration configurationObject) {
        boolean defaultSet = false;
        final List<ConfiguredLimitGroup> newLimitGroups = new ArrayList<ConfiguredLimitGroup>();

        for (ConfiguredLimitGroup limitGroup : configurationObject.getLimitGroup()) {
            // Makes sure that only the first limit group set to default is the only default group
            if (limitGroup.isDefault()) {

                if (defaultSet) {
                    limitGroup.setDefault(false);
                    LOG.warn("Rate-limiting Configuration has more than one default group set. Limit Group '"
                            + limitGroup.getId() + "' will not be set as a default limit group. Please update your configuration file.");
                } else {
                    defaultSet = true;
                }
            }

            // Create new limit groups that contain limit wrappers which each contains the pre-compiled uri regex Pattern
            final ConfiguredLimitGroup newLimitGroup = deepCopyLimitGroup(limitGroup);
            newLimitGroup.getLimit().clear();

            for (ConfiguredRatelimit configuredRatelimit : limitGroup.getLimit()) {
                final ConfiguredRatelimit newLimit = new ConfiguredRateLimitWrapper(configuredRatelimit);
                newLimitGroup.getLimit().add(newLimit);
            }

            newLimitGroups.add(newLimitGroup);
        }

        if (!defaultSet && configurationObject.getGlobalLimitGroup() == null) {
            LOG.warn("None of the specified rate limit groups have the default parameter set, and a global limit group has not been defined." +
                    " Running without a default or global rate limiting group is dangerous! Please update your config.");
        }

        return newLimitGroups;
    }

    private ConfiguredLimitGroup deepCopyLimitGroup(ConfiguredLimitGroup originalGroup) {
        final ConfiguredLimitGroup newGroup = new ConfiguredLimitGroup();

        newGroup.setDefault(originalGroup.isDefault());
        newGroup.setId(originalGroup.getId());

        final List<String> newRoleList = new ArrayList<String>(originalGroup.getGroups());
        Collections.copy(newRoleList, originalGroup.getGroups());
        newGroup.getGroups().addAll(newRoleList);

        final List<ConfiguredRatelimit> newLimits = new ArrayList<ConfiguredRatelimit>(originalGroup.getLimit());
        Collections.copy(newLimits, originalGroup.getLimit());
        newGroup.getLimit().addAll(newLimits);

        return newGroup;
    }

    private GlobalLimitGroup processGlobalLimits(GlobalLimitGroup oldGlobalLimitGroup) {
        GlobalLimitGroup newGlobalLimitGroup = new GlobalLimitGroup();

        for (ConfiguredRatelimit configuredRatelimit : oldGlobalLimitGroup.getLimit()) {
            ConfiguredRatelimit newLimit = new ConfiguredRateLimitWrapper(configuredRatelimit);
            newGlobalLimitGroup.getLimit().add(newLimit);
        }

        return newGlobalLimitGroup;
    }
}
