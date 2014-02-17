package com.rackspace.repose.service.ratelimit.cache;

import com.rackspace.repose.service.ratelimit.cache.util.TimeUnitConverter;
import com.rackspace.repose.service.ratelimit.config.ConfiguredRatelimit;

import java.io.Serializable;

/**
 *
 * @author jhopper
 */
public class CachedRateLimit implements Serializable {
    private final int maxCount;
    private final long unit;

    private int count;
    private long timestamp;

    public CachedRateLimit(ConfiguredRatelimit cfg) {
        this.maxCount = cfg.getValue();
        this.unit = TimeUnitConverter.fromSchemaTypeToConcurrent(cfg.getUnit()).toMillis(1);

        this.count = 0;
        this.timestamp = now();
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public void logHit() {
        vacuum();

        ++count;
    }

    public int amount() {
        vacuum();

        return count;
    }

    public long getSoonestRequestTime() {
        vacuum();

        if (count < maxCount) {
            return now();
        } else {
            return timestamp + unit;
        }
    }

    public long getNextExpirationTime() {
        vacuum();

        if (count != 0) {
            return timestamp + unit;
        } else {
            return now();
        }
    }

    private void vacuum() {
        final long now = now();

        if (now > timestamp + unit) {
            count = 0;
            timestamp = now;
        }
    }
}
