package org.openrepose.services.ratelimit.cache;

import org.openrepose.services.ratelimit.cache.util.TimeUnitConverter;
import org.openrepose.services.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;

/**
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {

    private final int maxCount;
    private final long unit;
    private final String configId;

    private int count;
    private long timestamp;

    public CachedRateLimit(ConfiguredRatelimit cfg) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configId = cfg.getId();

        this.count = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public CachedRateLimit(ConfiguredRatelimit cfg, int count) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configId = cfg.getId();

        this.count = count;
        this.timestamp = System.currentTimeMillis();
    }

    public CachedRateLimit(ConfiguredRatelimit cfg, int count, long timestamp) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);
        this.configId = cfg.getId();

        this.count = count;
        this.timestamp = timestamp;
    }

    public int maxAmount() {
        return maxCount;
    }

    public long unit() {
        return unit;
    }

    public String getConfigId() {
        return configId;
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
