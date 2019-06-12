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
package org.openrepose.core.services.ratelimit.cache;

import org.openrepose.core.services.ratelimit.cache.util.TimeUnitConverter;
import org.openrepose.core.services.ratelimit.config.ConfiguredRatelimit;

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
