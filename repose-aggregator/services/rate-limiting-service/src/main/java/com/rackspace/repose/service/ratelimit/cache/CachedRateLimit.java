package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.ratelimit.LimitKey;
import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;

/**
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {

    private final int maxCount;
    private final long unit;
    private final String configLimitKey;

    private int count;
    private long timestamp;

    public CachedRateLimit(ConfiguredRatelimit cfg) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configLimitKey = LimitKey.getConfigLimitKey(cfg.getUriRegex(), cfg.getHttpMethods());

        this.count = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public CachedRateLimit(ConfiguredRatelimit cfg, int count) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configLimitKey = LimitKey.getConfigLimitKey(cfg.getUriRegex(), cfg.getHttpMethods());

        this.count = count;
        this.timestamp = System.currentTimeMillis();
    }

    public CachedRateLimit(ConfiguredRatelimit cfg, int count, long timestamp) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configLimitKey = LimitKey.getConfigLimitKey(cfg.getUriRegex(), cfg.getHttpMethods());

        this.count = count;
        this.timestamp = timestamp;
    }

    public int maxAmount() {
        return maxCount;
    }

    public long unit() {
        return unit;
    }

    public String getConfigLimitKey() {
        return configLimitKey;
    }

    public long timestamp() {
        vacuum();

        return timestamp;
    }

    public int amount() {
        vacuum();

        return count;
    }

    public void logHit() {
        vacuum();

        ++count;
    }

    public long getSoonestRequestTime() {
        vacuum();

        if (count < maxCount) {
            return System.currentTimeMillis();
        } else {
            return timestamp + unit;
        }
    }

    public long getNextExpirationTime() {
        vacuum();

        return timestamp + unit;
    }

    private void vacuum() {
        final long now = System.currentTimeMillis();

        if (now > timestamp + unit) {
            count = 0;
            timestamp = now;
        }
    }
}
